package net.kenevans.maplines.lines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.kenevans.maplines.lines.MapCalibration.MapData;
import net.kenevans.maplines.utils.SWTUtils;

public class GPSLUtils
{
    private static final String GPSLINK_ID = "!GPSLINK";

    public static void saveGPSLMapFile(String fileName, String imageFileName,
        MapCalibration mapCalibration) throws Exception {
        // Use this to avoid the possibility of mixed CF and CRLF
        final String ls = SWTUtils.LS;
        final String delimiter = "\t";
        String timeStamp;
        double offset = 0;
        GregorianCalendar gcal;
        XMLGregorianCalendar xgcal;

        // Assume the file is not null and any asking to overwrite has been done
        // already
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            File file = new File(fileName);
            file.createNewFile();
            in = new BufferedReader(new FileReader(file));
            out = new PrintWriter(new FileWriter(file));
            // Use the offset for the current time in the current time zone.
            xgcal = null;
            try {
                // Make a new local GregorianCalendar with the current date
                gcal = new GregorianCalendar();
                gcal.setTime(new Date());
                // Make a new local XMLGregorianCalendar
                xgcal = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gcal);
                // Get its offset
                offset = xgcal.getTimezone() / 60.;
            } catch(Throwable t) {
                // Do nothing, use 0
            }

            // Print header
            out.print(GPSLINK_ID + ls);
            timeStamp = SWTUtils.timeStamp("MMM dd, yyyy hh:mm:ssa");
            // Convert AM/PM
            if(timeStamp.substring(21, 22).equalsIgnoreCase("P")) {
                timeStamp = timeStamp.substring(0, 21) + "p";
            } else {
                timeStamp = timeStamp.substring(0, 21) + "a";
            }
            out.print("Saved " + timeStamp + ls);
            out.print("Delimiter=" + delimiter + ls);
            // This prints e.g. -5.0 instead of -5, but leave it
            out.print("GMTOffset=" + offset + ls);

            out.print(ls);
            out.print("Map" + ls);
            File imageFile = new File(imageFileName);
            // Could use the full path name here. The short name works if the
            // .gpsl and image files are in the same directory.
            // later.
            out.print("M" + delimiter + imageFile.getName() + ls);
            int i = 1;
            String pointName;
            for(MapData data : mapCalibration.getDataList()) {
                pointName = "Point" + i++;
                out.print(String.format("C%s%S%S%.6f%s%.6f%s%d%s%d", delimiter,
                    pointName, delimiter, data.getLat(), delimiter,
                    data.getLon(), delimiter, data.getX(), delimiter,
                    data.getY()) + ls);
            }
        } catch(Exception ex) {
            throw ex;
        } finally {
            try {
                if(in != null) in.close();
                if(out != null) out.close();
            } catch(IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
    }

    /**
     * Converts a XMLGregorianCalendar and an offset to a GPSL trackpoint time
     * string.
     * 
     * @param xgcal The XMLGregorianCalendar.
     * @param offset The double offset in hours.
     * @return
     */
    public static String getTimeFromXMLGregorianCalendar(
        XMLGregorianCalendar xgcal, double offset) {
        GregorianCalendar gcal = xgcal
            .toGregorianCalendar(TimeZone.getTimeZone("GMT"), null, null);
        gcal.add(GregorianCalendar.MINUTE, (int)Math.round(60. * offset));
        // Don't use SimpleDateFormat("MM/dd/yyyy HH:mm:ss") It will format with
        // the current time zone, Use the values for MONTH, etc. from the gcal.
        // (Can't use the ones from the xgcal because they can't be
        // incremented.)
        String time = String.format("%02d/%02d/%04d %02d:%02d:%02d",
            gcal.get(GregorianCalendar.MONTH) + 1,
            gcal.get(GregorianCalendar.DAY_OF_MONTH),
            gcal.get(GregorianCalendar.YEAR),
            gcal.get(GregorianCalendar.HOUR_OF_DAY),
            gcal.get(GregorianCalendar.MINUTE),
            gcal.get(GregorianCalendar.SECOND));
        return time;
    }

}