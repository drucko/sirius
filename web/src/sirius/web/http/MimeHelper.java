/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.web.http;

import com.google.common.collect.Sets;
import com.google.common.io.Files;
import sirius.kernel.commons.Strings;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Guesses mime types based on file extensions.
 * <p>
 * Contains an internal table of the most common file extensions along with their mime type.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2013/08
 */
public class MimeHelper {
    /**
     * Mime type of flash (swf) files.
     */
    public static final String APPLICATION_X_SHOCKWAVE_FLASH = "application/x-shockwave-flash";

    /**
     * Mime type of PNG images
     */
    public static final String IMAGE_PNG = "image/png";

    /**
     * Mime type of JPEG images
     */
    public static final String IMAGE_JPEG = "image/jpeg";

    /**
     * Mime type of PDF files
     */
    public static final String APPLICATION_PDF = "application/pdf".intern();

    /**
     * Mime type of plain text files
     */
    public static final String TEXT_PLAIN = "text/plain";

    /**
     * Mime type of CSS files
     */
    public static final String TEXT_CSS = "text/css".intern();

    /**
     * Mime type of HTML files
     */
    public static final String TEXT_HTML = "text/html".intern();

    /**
     * Mime type of XML files
     */
    public static final String TEXT_XML = "text/xml".intern();

    /**
     * Mime type of csv files
     */
    public static final String TEXT_CSV = "text/comma-separated-values".intern();

    /**
     * Mime type of javascript (JS) files
     */
    public static final String TEXT_JAVASCRIPT = "text/javascript".intern();

    /**
     * Mime type of mpeg sound files (MP3)
     */
    private static final String AUDIO_MPEG = "audio/mpeg";

    /**
     * Mime type of mpeg video files
     */
    private static final String VIDEO_MPEG = "video/mpeg";

    /**
     * Mime type of ogg vorbis video files
     */
    private static final String VIDEO_OGG = "video/ogg";

    /**
     * Mime type of mpeg 4 video files
     */
    private static final String VIDEO_MP4 = "video/mp4";

    /**
     * Mime type of Blu-ray Disc Audio-Video (BDAV) MPEG-2 Transport Stream (M2TS)
     */
    private static final String VIDEO_MP2T = "video/MP2T";

    /**
     * Mime type of zip files
     */
    private static final String APPLICATION_ZIP = "application/zip";

    /**
     * Mime type of quicktime videos
     */
    private static final String VIDEO_QUICKTIME = "video/quicktime";

    private static final Map<String, String> mimeTable = new TreeMap<String, String>();

    /*
     * The list is limited to the most common mime types known to be compressable. Compressing already compressed
     * content does not harm other than wasting some CPU cycles.
     */
    private static final Set<String> COMPRESSABLE = Sets.newHashSet(TEXT_CSS,
                                                                    TEXT_JAVASCRIPT,
                                                                    TEXT_HTML,
                                                                    TEXT_CSV,
                                                                    TEXT_XML);

    static {
        mimeTable.put("ai", "application/postscript");
        mimeTable.put("aif", "audio/x-aiff");
        mimeTable.put("aifc", "audio/x-aiff");
        mimeTable.put("aiff", "audio/x-aiff");
        mimeTable.put("asc", TEXT_PLAIN);
        mimeTable.put("atom", "application/atom+xml");
        mimeTable.put("au", "audio/basic");
        mimeTable.put("avi", "video/x-msvideo");
        mimeTable.put("bcpio", "application/x-bcpio");
        mimeTable.put("bin", "application/octet-stream");
        mimeTable.put("bmp", "image/bmp");
        mimeTable.put("cdf", "application/x-netcdf");
        mimeTable.put("cgm", "image/cgm");
        mimeTable.put("class", "application/octet-stream");
        mimeTable.put("cpio", "application/x-cpio");
        mimeTable.put("cpt", "application/mac-compactpro");
        mimeTable.put("csh", "application/x-csh");
        mimeTable.put("css", TEXT_CSS);
        mimeTable.put("csv", TEXT_CSV);
        mimeTable.put("dcr", "application/x-director");
        mimeTable.put("dif", "video/x-dv");
        mimeTable.put("dir", "application/x-director");
        mimeTable.put("djv", "image/vnd.djvu");
        mimeTable.put("djvu", "image/vnd.djvu");
        mimeTable.put("dll", "application/octet-stream");
        mimeTable.put("dmg", "application/octet-stream");
        mimeTable.put("dms", "application/octet-stream");
        mimeTable.put("doc", "application/msword");
        mimeTable.put("dtd", "application/xml-dtd");
        mimeTable.put("dv", "video/x-dv");
        mimeTable.put("dvi", "application/x-dvi");
        mimeTable.put("dxr", "application/x-director");
        mimeTable.put("eps", "application/postscript");
        mimeTable.put("etx", "text/x-setext");
        mimeTable.put("exe", "application/octet-stream");
        mimeTable.put("ez", "application/andrew-inset");
        mimeTable.put("gif", "image/gif");
        mimeTable.put("gram", "application/srgs");
        mimeTable.put("grxml", "application/srgs+xml");
        mimeTable.put("gtar", "application/x-gtar");
        mimeTable.put("hdf", "application/x-hdf");
        mimeTable.put("hqx", "application/mac-binhex40");
        mimeTable.put("htm", TEXT_HTML);
        mimeTable.put("html", TEXT_HTML);
        mimeTable.put("ice", "x-conference/x-cooltalk");
        mimeTable.put("ico", "image/x-icon");
        mimeTable.put("ics", "text/calendar");
        mimeTable.put("ief", "image/ief");
        mimeTable.put("ifb", "text/calendar");
        mimeTable.put("iges", "model/iges");
        mimeTable.put("igs", "model/iges");
        mimeTable.put("jnlp", "application/x-java-jnlp-file");
        mimeTable.put("jp2", "image/jp2");
        mimeTable.put("jpe", IMAGE_JPEG);
        mimeTable.put("jpeg", IMAGE_JPEG);
        mimeTable.put("jpg", IMAGE_JPEG);
        mimeTable.put("js", TEXT_JAVASCRIPT);
        mimeTable.put("kar", "audio/midi");
        mimeTable.put("latex", "application/x-latex");
        mimeTable.put("lha", "application/octet-stream");
        mimeTable.put("log", "text/plain");
        mimeTable.put("lzh", "application/octet-stream");
        mimeTable.put("m3u", "audio/x-mpegurl");
        mimeTable.put("m4a", "audio/mp4a-latm");
        mimeTable.put("m4b", "audio/mp4a-latm");
        mimeTable.put("m4p", "audio/mp4a-latm");
        mimeTable.put("m4u", "video/vnd.mpegurl");
        mimeTable.put("m4v", "video/x-m4v");
        mimeTable.put("mac", "image/x-macpaint");
        mimeTable.put("man", "application/x-troff-man");
        mimeTable.put("mathml", "application/mathml+xml");
        mimeTable.put("me", "application/x-troff-me");
        mimeTable.put("mesh", "model/mesh");
        mimeTable.put("mid", "audio/midi");
        mimeTable.put("midi", "audio/midi");
        mimeTable.put("mif", "application/vnd.mif");
        mimeTable.put("mov", VIDEO_QUICKTIME);
        mimeTable.put("movie", "video/x-sgi-movie");
        mimeTable.put("mp2", AUDIO_MPEG);
        mimeTable.put("mp3", AUDIO_MPEG);
        mimeTable.put("mp4", VIDEO_MP4);
        mimeTable.put("mpe", VIDEO_MPEG);
        mimeTable.put("mpeg", VIDEO_MPEG);
        mimeTable.put("mpg", VIDEO_MPEG);
        mimeTable.put("mpga", AUDIO_MPEG);
        mimeTable.put("ms", "application/x-troff-ms");
        mimeTable.put("msh", "model/mesh");
        mimeTable.put("mxu", "video/vnd.mpegurl");
        mimeTable.put("nc", "application/x-netcdf");
        mimeTable.put("oda", "application/oda");
        mimeTable.put("ogg", VIDEO_OGG);
        mimeTable.put("ogv", VIDEO_OGG);
        mimeTable.put("pbm", "image/x-portable-bitmap");
        mimeTable.put("pct", "image/pict");
        mimeTable.put("pdb", "chemical/x-pdb");
        mimeTable.put("pdf", "application/pdf");
        mimeTable.put("pgm", "image/x-portable-graymap");
        mimeTable.put("pgn", "application/x-chess-pgn");
        mimeTable.put("pic", "image/pict");
        mimeTable.put("pict", "image/pict");
        mimeTable.put("png", IMAGE_PNG);
        mimeTable.put("pnm", "image/x-portable-anymap");
        mimeTable.put("pnt", "image/x-macpaint");
        mimeTable.put("pntg", "image/x-macpaint");
        mimeTable.put("ppm", "image/x-portable-pixmap");
        mimeTable.put("ppt", "application/vnd.ms-powerpoint");
        mimeTable.put("ps", "application/postscript");
        mimeTable.put("qt", VIDEO_QUICKTIME);
        mimeTable.put("qti", "image/x-quicktime");
        mimeTable.put("qtif", "image/x-quicktime");
        mimeTable.put("ra", "audio/x-pn-realaudio");
        mimeTable.put("ram", "audio/x-pn-realaudio");
        mimeTable.put("ras", "image/x-cmu-raster");
        mimeTable.put("rdf", "application/rdf+xml");
        mimeTable.put("rgb", "image/x-rgb");
        mimeTable.put("rm", "application/vnd.rn-realmedia");
        mimeTable.put("roff", "application/x-troff");
        mimeTable.put("rtf", "text/rtf");
        mimeTable.put("rtx", "text/richtext");
        mimeTable.put("sgm", "text/sgml");
        mimeTable.put("sgml", "text/sgml");
        mimeTable.put("sh", "application/x-sh");
        mimeTable.put("shar", "application/x-shar");
        mimeTable.put("silo", "model/mesh");
        mimeTable.put("sit", "application/x-stuffit");
        mimeTable.put("skd", "application/x-koan");
        mimeTable.put("skm", "application/x-koan");
        mimeTable.put("skp", "application/x-koan");
        mimeTable.put("skt", "application/x-koan");
        mimeTable.put("smi", "application/smil");
        mimeTable.put("smil", "application/smil");
        mimeTable.put("snd", "audio/basic");
        mimeTable.put("so", "application/octet-stream");
        mimeTable.put("spl", "application/x-futuresplash");
        mimeTable.put("src", "application/x-wais-source");
        mimeTable.put("sv4cpio", "application/x-sv4cpio");
        mimeTable.put("sv4crc", "application/x-sv4crc");
        mimeTable.put("svg", "image/svg+xml");
        mimeTable.put("swf", APPLICATION_X_SHOCKWAVE_FLASH);
        mimeTable.put("t", "application/x-troff");
        mimeTable.put("tar", "application/x-tar");
        mimeTable.put("tcl", "application/x-tcl");
        mimeTable.put("tex", "application/x-tex");
        mimeTable.put("texi", "application/x-texinfo");
        mimeTable.put("texinfo", "application/x-texinfo");
        mimeTable.put("tif", "image/tiff");
        mimeTable.put("tiff", "image/tiff");
        mimeTable.put("tr", "application/x-troff");
        mimeTable.put("tsv", "text/tab-separated-values");
        mimeTable.put("txt", "text/plain");
        mimeTable.put("ustar", "application/x-ustar");
        mimeTable.put("vcd", "application/x-cdlink");
        mimeTable.put("vrml", "model/vrml");
        mimeTable.put("vxml", "application/voicexml+xml");
        mimeTable.put("wav", "audio/x-wav");
        mimeTable.put("wbmp", "image/vnd.wap.wbmp");
        mimeTable.put("wbmxl", "application/vnd.wap.wbxml");
        mimeTable.put("wml", "text/vnd.wap.wml");
        mimeTable.put("wmlc", "application/vnd.wap.wmlc");
        mimeTable.put("wmls", "text/vnd.wap.wmlscript");
        mimeTable.put("wmlsc", "application/vnd.wap.wmlscriptc");
        mimeTable.put("wrl", "model/vrml");
        mimeTable.put("xbm", "image/x-xbitmap");
        mimeTable.put("xht", "application/xhtml+xml");
        mimeTable.put("xhtml", "application/xhtml+xml");
        mimeTable.put("xls", "application/msexcel");
        mimeTable.put("xlsx", "application/msexcel");
        mimeTable.put("xml", TEXT_XML);
        mimeTable.put("xpm", "image/x-xpixmap");
        mimeTable.put("xsl", "application/xml");
        mimeTable.put("xslt", "application/xslt+xml");
        mimeTable.put("xul", "application/vnd.mozilla.xul+xml");
        mimeTable.put("xwd", "image/x-xwindowdump");
        mimeTable.put("xyz", "chemical/x-xyz");
        mimeTable.put("zip", APPLICATION_ZIP);

    }

    /**
     * Tries to guess the mime type for the given file, path or url based on its file ending.
     *
     * @param name the filename, path or URL to use to detect the mime type
     * @return the mime type or <tt>null</tt> if the input was <tt>null</tt>
     */
    public static String guessMimeType(String name) {
        if (Strings.isEmpty(name)) {
            return null;
        }
        // Fast lookup for common types....
        if (name.length() >= 4 && name.charAt(name.length() - 4) == '.') {
            String ending = name.substring(name.length() - 3).toLowerCase().intern();
            if ("jpg" == ending) {
                return IMAGE_JPEG;
            }
            if ("swf" == ending) {
                return APPLICATION_X_SHOCKWAVE_FLASH;
            }
            if ("pdf" == ending) {
                return APPLICATION_PDF;
            }
            if ("png" == ending) {
                return IMAGE_PNG;
            }
            if ("css" == ending) {
                return TEXT_CSS;
            }
            if ("xml" == ending) {
                return TEXT_JAVASCRIPT;
            }
            if ("txt" == ending) {
                return TEXT_JAVASCRIPT;
            }
        }

        name = Files.getFileExtension(name).toLowerCase();
        String result = mimeTable.get(name);
        if (result == null) {
            return "application/octet-stream";
        } else {
            return result;
        }
    }

    /**
     * Determines if it is recommended to compress data of the given mime type.
     * <p>
     * It isn't very useful to compress JPEG or PNG files, as they are already compressed. This method is very
     * pessimistic: Only a set of known content types is accepted as compressable.
     * </p>
     *
     * @param contentType the mime type to check
     * @return <tt>true</tt> if the mime type is recognized as compressable, <tt>false</tt> otherwise
     */
    public static boolean isCompressable(String contentType) {
        if (contentType == null) {
            return false;
        }
        int idx = contentType.indexOf(";");
        if (idx >= 0) {
            contentType = contentType.substring(0, idx);
        }
        return contentType != null && COMPRESSABLE.contains(contentType);
    }
}
