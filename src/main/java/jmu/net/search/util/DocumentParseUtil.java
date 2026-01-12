package jmu.net.search.util;

import jmu.net.search.constant.FileConstant;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DocumentParseUtil {

    public static String parseFileContent(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        try (InputStream inputStream = new FileInputStream(file)) {
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString().trim();
        } catch (IOException | TikaException | SAXException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean isSupportFile(String fileName) {
        String suffix = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        for (String s : FileConstant.TXT_SUFFIX) {
            if (s.equals(suffix)) {
                return true;
            }
        }
        for (String s : FileConstant.OFFICE_SUFFIX) {
            if (s.equals(suffix)) {
                return true;
            }
        }
        return false;
    }
}