package net.kenevans.maplines.lines;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.swt.graphics.Point;

import net.kenevans.maplines.utils.SWTUtils;

/*
 * Created on Aug 20, 2013
 * By Kenneth Evans, Jr.
 */

public class GPXUtils
{
    /** The default speed to use for calculating time differences (mph). */
    public static double DEFAULT_SPEED = 10;

    /**
     * Nominal radius of the earth in miles. The radius actually varies from
     * 3937 to 3976 mi.
     */
    public static final double REARTH = 3956;
    /** Multiplier to convert miles to nautical miles. */
    public static final double MI2NMI = 1.852; // Exact
    /** Multiplier to convert degrees to radians. */
    public static final double DEG2RAD = Math.PI / 180.;
    /** Multiplier to convert feet to miles. */
    public static final double FT2MI = 1. / 5280.;
    /** Multiplier to convert meters to miles. */
    public static final double M2MI = .00062137119224;
    /** Multiplier to convert kilometers to miles. */
    public static final double KM2MI = .001 * M2MI;
    /** Multiplier to convert meters to feet. */
    public static final double M2FT = 3.280839895;
    /** Multiplier to convert sec to hours. */
    public static final double SEC2HR = 1. / 3600.;
    /** Multiplier to convert millisec to hours. */
    public static final double MS2HR = .001 * SEC2HR;

    public static final String COMMA = ",";

    public static void writeGPXFile(String fileName, String trackName,
        MapCalibration mapCalibration, Lines lines) {
        File file = new File(fileName);
        boolean doIt = true;
        if(file.exists()) {
            Boolean res = SWTUtils.confirmMsg(
                "File exists: " + file.getPath() + "\nOK to overwrite?");
            if(!res) {
                doIt = false;
            }
        }
        if(doIt) {
            PrintWriter out = null;
            try {
                Date date = new Date();
                // TEMP Use to generate GPX with specific start time and speed.
                // Requires change in timeString and some printout below
                // Search for TEMP
                // date = new
                // SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2020-03-21T15:51:33.002");
                // DEFAULT_SPEED = 1.524153;

                out = new PrintWriter(new FileWriter(file));
                // Write header
                out.println(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
                out.println("<gpx");
                out.println(" creator=\"MapLines\"");
                out.println(" version=\"1.1\"");
                out.println(" xmlns=\"http://www.topografix.com/GPX/1/1\"");
                out.println(
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
                out.println(
                    " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 ");
                out.println("   http://www.topografix.com/GPX/1/1/gpx.xsd\">");

                // Write metadata
                out.println("  <metadata>");
                out.println(
                    String.format("    <time>%s</time>", timeString(date)));
                out.println("  </metadata>");

                // Write lines
                if(lines != null) {
                    Line line;
                    Point point;
                    out.println("  <trk>");
                    out.println("    <name>" + trackName + "</name>");
                    out.println("    <extensions>");
                    out.println(
                        "      <gpxx:TrackExtension xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\">");
                    out.println(
                        "        <gpxx:DisplayColor>Blue</gpxx:DisplayColor>");
                    out.println("      </gpxx:TrackExtension>");
                    out.println("    </extensions>");
                    double lon0 = 0, lat0 = 0;
                    double dist = 0;
                    double distTot = 0;
                    long time = 0;
                    for(int i = 0; i < lines.getNLines(); i++) {
                        line = lines.getLines().get(i);
                        out.println("    <trkseg>");
                        for(int j = 0; j < line.getNPoints(); j++) {
                            point = line.getPoints().get(j);
                            double[] vals = mapCalibration.transform(point.x,
                                point.y);
                            out.println(String.format(
                                "      <trkpt lat=\"%f\" lon=\"%f\">", vals[1],
                                vals[0]));
                            out.println("        <ele>0.0</ele>");
                            // Increment the time assuming a default speed
                            if(j == 0) {
                                lon0 = vals[0];
                                lat0 = vals[1];
                            } else {
                                dist = Math.abs(GPXUtils.M2MI
                                    * GPXUtils.greatCircleDistance(lat0, lon0,
                                        vals[1], vals[0]));
                                time = (long)(dist / DEFAULT_SPEED * 3600.
                                    * 1000.);
                                date.setTime(date.getTime() + time);
                                // System.out.println("dist=" + dist + " time="
                                // + time + " (" + time / 1000 + " sec)");
                                lon0 = vals[0];
                                lat0 = vals[1];
                            }
                            distTot += dist;
                            out.println(String.format("        <time>%s</time>",
                                timeString(date)));
                            out.println("      </trkpt>");
                        }
                        out.println("    </trkseg>");
                    }
                    out.println("  </trk>");
                    out.println("</gpx>");
                    System.out.println("distTot=" + distTot + " speed="
                        + DEFAULT_SPEED + " endTime=" + timeString(date));
                }
            } catch(Exception ex) {
                SWTUtils.excMsg("Error writing " + fileName, ex);
            } finally {
                if(out != null) out.close();
                out = null;
            }
        }
    }

    public static void writeCSVFile(String fileName,
        MapCalibration mapCalibration, Lines lines) {
        File file = new File(fileName);
        boolean doIt = true;
        if(file.exists()) {
            Boolean res = SWTUtils.confirmMsg(
                "File exists: " + file.getPath() + "\nOK to overwrite?");
            if(!res) {
                doIt = false;
            }
        }
        if(doIt) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(file));
                // Write header
                String[] headings = {"Name", "X", "Y", "x", "y"};
                boolean first = true;
                for(String heading : headings) {
                    if(!first) {
                        out.print(COMMA);
                    }
                    out.print(heading);
                    first = false;
                }
                out.println();
                // Write lines
                if(lines != null) {
                    Line line;
                    Point point;
                    for(int i = 0; i < lines.getNLines(); i++) {
                        line = lines.getLines().get(i);
                        for(int j = 0; j < line.getNPoints(); j++) {
                            point = line.getPoints().get(j);
                            double[] vals = mapCalibration.transform(point.x,
                                point.y);
                            out.println(String.format("\"%s\"%s%f%s%f%s%d%s%d",
                                line.getDesc(), COMMA, vals[0], COMMA, vals[1],
                                COMMA, point.x, COMMA, point.y));
                        }
                    }
                }
            } catch(Exception ex) {
                SWTUtils.excMsg("Error writing " + fileName, ex);
            } finally {
                if(out != null) out.close();
                out = null;
            }
        }
    }

    public static String timeString(Date date) {
        if(date == null) {
            return "NA";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
        // TEMP Use to generate GPX with specific format.
        // Comment out formatter.setTimeZone
        // formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(date);
    }

    /**
     * Returns great circle distance in meters. assuming a spherical earth. Uses
     * Haversine formula.
     * 
     * @param lat1 Start latitude in deg.
     * @param lon1 Start longitude in deg.
     * @param lat2 End latitude in deg.
     * @param lon2 End longitude in deg.
     * @return
     */
    public static double greatCircleDistance(double lat1, double lon1,
        double lat2, double lon2) {
        double slon, slat, a, c, d;

        // Convert to radians
        lat1 *= DEG2RAD;
        lon1 *= DEG2RAD;
        lat2 *= DEG2RAD;
        lon2 *= DEG2RAD;

        // Haversine formula
        slon = Math.sin((lon2 - lon1) / 2.);
        slat = Math.sin((lat2 - lat1) / 2.);
        a = slat * slat + Math.cos(lat1) * Math.cos(lat2) * slon * slon;
        c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        d = REARTH / M2MI * c;

        return (d);
    }

}
