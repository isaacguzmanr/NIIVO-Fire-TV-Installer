package com.nivo.installer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads only the latest official GitHub Release and its explicitly versioned NIIVO APK. */
final class UpdateManager {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");
    private static final Pattern APK_NAME = Pattern.compile("(?i)^NIIVO[-_]v?(\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?)\\.apk$");

    interface DownloadListener { void onProgress(long downloaded, long total); }

    static final class Release {
        final String version, tag, apkName, downloadUrl, sha256, htmlUrl;
        final long size;
        Release(String version, String tag, String apkName, String downloadUrl, long size, String sha256, String htmlUrl) {
            this.version=version; this.tag=tag; this.apkName=apkName; this.downloadUrl=downloadUrl;
            this.size=size; this.sha256=sha256; this.htmlUrl=htmlUrl;
        }
    }
    static final class ReleaseCheck {
        final String latestVersion, tag, htmlUrl;
        final Release asset;
        ReleaseCheck(String version, String tag, String htmlUrl, Release asset) {
            latestVersion=version; this.tag=tag; this.htmlUrl=htmlUrl; this.asset=asset;
        }
    }

    static ReleaseCheck checkLatest() throws Exception {
        String endpoint="https://api.github.com/repos/"+UpdateConfig.GITHUB_OWNER+"/"+UpdateConfig.GITHUB_REPOSITORY+"/releases?per_page=20";
        HttpURLConnection c=open(endpoint,"application/vnd.github+json");
        try {
            int code=c.getResponseCode();
            if(code==404) throw new java.io.IOException("El repositorio no tiene un release publicado.");
            if(code<200||code>=300) throw new java.io.IOException("GitHub respondió con HTTP "+code+".");
            Release found=null;
            JSONArray releases=new JSONArray(readText(c.getInputStream()));
            for(int r=0;r<releases.length();r++) {
                JSONObject json=releases.optJSONObject(r); if(json==null||json.optBoolean("draft",false)) continue;
                String tag=json.optString("tag_name","").trim();
                String version=normalizeVersion(tag);
                if(version==null) continue;
                String page=json.optString("html_url","");
                JSONArray assets=json.optJSONArray("assets");
                if(assets==null) continue;
                for(int i=0;i<assets.length();i++) {
                    JSONObject a=assets.optJSONObject(i); if(a==null) continue;
                    String name=a.optString("name",""); Matcher m=APK_NAME.matcher(name);
                    if(!m.matches()||!version.equals(normalizeVersion(m.group(1)))) continue;
                    String url=a.optString("browser_download_url","");
                    if(!isExpectedAssetUrl(url,tag,name)) continue;
                    if(found==null||compareVersions(version,found.version)>0)
                        found=new Release(version,tag,name,url,a.optLong("size",0L),findChecksum(assets,name,tag),page);
                    break;
                }
            }
            if(found==null) throw new java.io.IOException("No se encontró un APK NIIVO válido en los releases publicados.");
            return new ReleaseCheck(found.version,found.tag,found.htmlUrl,found);
        } finally { c.disconnect(); }
    }

    static File download(Release release, File destination, DownloadListener listener) throws Exception {
        HttpURLConnection c=open(release.downloadUrl,"application/octet-stream");
        try {
            int code=c.getResponseCode();
            if(code<200||code>=300) throw new java.io.IOException("No se pudo descargar el APK (HTTP "+code+").");
            long total=c.getContentLengthLong(); if(total<=0) total=release.size;
            long downloaded=0;
            try(InputStream in=new BufferedInputStream(c.getInputStream()); FileOutputStream out=new FileOutputStream(destination,false)) {
                byte[] buffer=new byte[64*1024]; int count;
                while((count=in.read(buffer))!=-1) {
                    out.write(buffer,0,count); downloaded+=count;
                    if(listener!=null) listener.onProgress(downloaded,total);
                }
                out.getFD().sync();
            }
            if(downloaded<=0) throw new java.io.IOException("El APK descargado está vacío.");
            if(release.size>0&&downloaded!=release.size) throw new java.io.IOException("El tamaño descargado no coincide con el asset publicado.");
            if(release.sha256!=null&&!release.sha256.isEmpty()&&!sha256(destination).equalsIgnoreCase(release.sha256))
                throw new java.io.IOException("El SHA-256 del APK no coincide con el checksum publicado.");
            return destination;
        } finally { c.disconnect(); }
    }

    static String normalizeVersion(String raw) {
        if(raw==null) return null;
        Matcher m=VERSION.matcher(raw.trim()); if(!m.matches()) return null;
        return m.group(1)+"."+m.group(2)+"."+m.group(3)+(m.group(4)==null?"":"-"+m.group(4));
    }
    static int compareVersions(String left,String right) {
        Matcher a=VERSION.matcher(left==null?"":left.trim()), b=VERSION.matcher(right==null?"":right.trim());
        if(!a.matches()||!b.matches()) throw new IllegalArgumentException("Versión semántica inválida.");
        for(int i=1;i<=3;i++){ int cmp=Long.compare(number(a.group(i)),number(b.group(i))); if(cmp!=0)return cmp; }
        String pa=a.group(4),pb=b.group(4); if(pa==null&&pb==null)return 0; if(pa==null)return 1; if(pb==null)return -1;
        String[] aa=pa.split("\\."),bb=pb.split("\\.");
        for(int i=0;i<Math.min(aa.length,bb.length);i++){
            boolean an=aa[i].matches("\\d+"),bn=bb[i].matches("\\d+"); int cmp;
            if(an&&bn)cmp=Long.compare(number(aa[i]),number(bb[i])); else if(an!=bn)cmp=an?-1:1; else cmp=aa[i].compareToIgnoreCase(bb[i]);
            if(cmp!=0)return cmp;
        }
        return Integer.compare(aa.length,bb.length);
    }
    private static long number(String value){try{return Long.parseLong(value);}catch(NumberFormatException e){return Long.MAX_VALUE;}}
    private static String findChecksum(JSONArray assets,String apkName,String tag) throws Exception {
        String[] candidates={apkName+".sha256", "NIIVO-"+normalizeVersion(tag)+"-SHA256SUMS.txt", "SHA256SUMS.txt"};
        for(int i=0;i<assets.length();i++){
            JSONObject a=assets.optJSONObject(i); if(a==null)continue;
            String checksumName=a.optString("name",""); boolean recognized=false;
            for(String candidate:candidates)if(candidate.equalsIgnoreCase(checksumName)){recognized=true;break;}
            if(!recognized)continue;
            String url=a.optString("browser_download_url","");
            if(!isExpectedAssetUrl(url,tag,checksumName)) throw new java.io.IOException("El checksum publicado no pertenece al release esperado.");
            HttpURLConnection c=open(url,"application/octet-stream");
            try{
                int code=c.getResponseCode();
                if(code<200||code>=300)throw new java.io.IOException("No se pudo leer el checksum publicado (HTTP "+code+".).");
                String contents=readText(c.getInputStream());
                for(String line:contents.split("\\r?\\n")){
                    if(!checksumName.equalsIgnoreCase(apkName+".sha256")&&!line.contains(apkName))continue;
                    Matcher m=Pattern.compile("(?i)([a-f0-9]{64})").matcher(line);
                    if(m.find())return m.group(1).toLowerCase(Locale.ROOT);
                }
                throw new java.io.IOException("El checksum publicado no contiene el SHA-256 del APK esperado.");
            }finally{c.disconnect();}
        }
        return null;
    }
    private static boolean isExpectedAssetUrl(String raw,String tag,String name){
        try{
            URL u=new URL(raw);
            return "https".equalsIgnoreCase(u.getProtocol())&&"github.com".equalsIgnoreCase(u.getHost())
                &&u.getPath().contains("/"+UpdateConfig.GITHUB_OWNER+"/"+UpdateConfig.GITHUB_REPOSITORY+"/releases/download/"+tag+"/")
                &&u.getPath().endsWith("/"+name);
        }catch(Exception e){return false;}
    }
    private static HttpURLConnection open(String url,String accept)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(CONNECT_TIMEOUT_MS); c.setReadTimeout(READ_TIMEOUT_MS);
        c.setRequestProperty("User-Agent","NIIVO-Android-Updater"); c.setRequestProperty("Accept",accept); c.setInstanceFollowRedirects(true); return c;
    }
    private static String readText(InputStream input)throws Exception{
        try(InputStream in=input;java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    private static String sha256(File file)throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new BufferedInputStream(new java.io.FileInputStream(file))){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}
        StringBuilder result=new StringBuilder(64);for(byte v:d.digest())result.append(String.format(Locale.ROOT,"%02x",v&255));return result.toString();
    }
    private UpdateManager(){}
}
