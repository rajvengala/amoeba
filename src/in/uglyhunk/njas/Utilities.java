/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package in.uglyhunk.njas;

/**
 *
 * @author uglyhunk
 */
public class Utilities {
    public static String stackTraceToString(Throwable e){
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append(EOL);
        for(StackTraceElement element : e.getStackTrace()) {
            sb.append("\t").append(element.toString());
            sb.append(EOL);
        }
        return sb.toString();
    }

    public static String getEOL(){
        return EOL;
    }

    public static String getHTTPEOL(){
        return HTTP_EOL;
    }

    private static final String EOL = System.getProperty("line.separator");
    private static final String HTTP_EOL = "\r\n";
}


