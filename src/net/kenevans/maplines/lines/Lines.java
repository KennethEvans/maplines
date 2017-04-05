package net.kenevans.maplines.lines;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.kenevans.maplines.utils.SWTUtils;

/*
 * Created on Aug 17, 2013
 * By Kenneth Evans, Jr.
 */

public class Lines
{
    private static final String START_LINES_TAG = "STARTLINE";
    private static final String END_LINES_TAG = "ENDLINE";
    private static boolean doTest = false;
    private List<Line> lines = new ArrayList<Line>();

    public Lines() {
        if(doTest) {
            lines.add(new Line(new int[][] {{405, 5}, {406, 110}, {412, 204},
                {420, 220}, {530, 220}, {5640, 330}}));
            lines.add(new Line(new int[][] {{-30, 135}, {220, 10}, {216, 204},
                {312, 320}, {415, 520}, {340, 630}}));
            lines.add(new Line(new int[][] {{435, 307}, {456, 305}, {522, 304},
                {532, 320}, {618, 325}, {540, 550}}));
        }
    }

    /**
     * Adds the given line.
     * 
     * @param line
     */
    public void addLine(Line line) {
        if(lines != null) {
            lines.add(line);
        }
    }

    /**
     * Removes the given line.
     * 
     * @param line
     */
    public void removeLine(Line line) {
        if(lines != null) {
            lines.remove(line);
        }
    }

    /**
     * Removes the line at index.
     * 
     * @param index
     */
    public void removeLine(int index) {
        if(lines != null) {
            lines.remove(index);
        }
    }

    /**
     * Removes all the lines.
     */
    public void clear() {
        if(lines != null) {
            lines.clear();
        }
    }

    /**
     * Gets the number of lines.
     * 
     * @return
     */
    public int getNLines() {
        return lines.size();
    }

    /**
     * Saves the lines to a file.
     * 
     * @param fileName The name of the file.
     */
    public void saveLines(String fileName) {
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
                if(lines != null) {
                    Line line;
                    Point point;
                    for(int i = 0; i < getNLines(); i++) {
                        line = lines.get(i);
                        if(line.getDesc() != null
                            && line.getDesc().length() != 0) {
                            out.println(START_LINES_TAG + " " + line.getDesc());
                        } else {
                            out.println(START_LINES_TAG);
                        }
                        for(int j = 0; j < line.getNPoints(); j++) {
                            point = line.getPoints().get(j);
                            out.println(point.x + " " + point.y);
                        }
                        out.println(END_LINES_TAG);
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

    /**
     * Saves the lines as an image to a file.
     * 
     * @param fileName The name of the file.
     */
    public void saveLinesImage(Display display, String fileName, int width,
        int height) {
        if(display == null) {
            SWTUtils.errMsg("Error creating image. Invalid map Display");
            return;
        }

        // Check the file
        File file = new File(fileName);
        boolean doIt = true;
        if(file.exists()) {
            Boolean res = SWTUtils.confirmMsg(
                "File exists: " + file.getPath() + "\nOK to overwrite?");
            if(!res) {
                doIt = false;
            }
        }
        if(!doIt) return;

        // Create the image
        try {
            BufferedImage bufferedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            
            // Make the background transparent
            g2d.setBackground(new Color(255, 255, 255, 0));
            g2d.clearRect(0, 0, width, height);

            // Draw the lines
            g2d.setColor(Color.RED);
            if(lines != null) {
                Line line;
                Point point;
                for(int i = 0; i < getNLines(); i++) {
                    line = lines.get(i);
                    int startX = 0, startY = 0;
                    for(int j = 0; j < line.getNPoints(); j++) {
                        point = line.getPoints().get(j);
                        if(j > 0) {
                            g2d.drawLine(startX, startY, point.x, point.y);
                        }
                        startX = point.x;
                        startY = point.y;
                    }
                }
            }
            g2d.dispose();

            // Save the image
            ImageIO.write(bufferedImage, "PNG", file);
        } catch(Exception ex) {
            SWTUtils.excMsg("Error writing image file", ex);
        }
    }

    /**
     * Reads lines from a file and adds them to the current lines.
     * 
     * @param fileName
     * @return
     * @throws NumberFormatException
     * @throws IOException
     */
    public boolean readLines(String fileName)
        throws NumberFormatException, IOException {
        boolean ok = false;
        BufferedReader in = null;
        String[] tokens = null;
        int x, y;
        in = new BufferedReader(new FileReader(fileName));
        String fileLine;
        Line line = null;
        int lineNum = 0;
        while((fileLine = in.readLine()) != null) {
            lineNum++;
            tokens = fileLine.trim().split("\\s+");
            // Skip blank lines
            if(tokens.length == 0) {
                continue;
            }
            // Skip lines starting with #
            if(tokens[0].trim().startsWith("#")) {
                continue;
            }
            if(tokens[0].equals(START_LINES_TAG)) {
                line = new Line();
                int end1 = START_LINES_TAG.length();
                int end2 = fileLine.length();
                if(end2 > end1) {
                    line.setDesc(fileLine.substring(end1).trim());
                }
                continue;
            }
            if(tokens[0].equals(END_LINES_TAG)) {
                if(line != null) {
                    addLine(line);
                    line = null;
                }
                continue;
            }
            // Must be 2 or more values, any after 2 are ignored
            if(tokens.length < 2) {
                SWTUtils.errMsg("Invalid Lines file at line " + lineNum);
                in.close();
                return false;
            }
            x = Integer.parseInt(tokens[0]);
            y = Integer.parseInt(tokens[1]);
            line.addPoint(new Point(x, y));
        }
        in.close();
        return ok;
    }

    /**
     * Reads lines from a GPX file and adds them to the current lines.
     * 
     * @param fileName
     * @return
     * @throws NumberFormatException
     * @throws IOException
     */
    public boolean readGpxLines(String fileName, MapCalibration mapCalibration)
        throws NumberFormatException, IOException {
        boolean ok = false;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
            .newInstance();
        int nTrkPoints = 0;
        FileInputStream fileInputStream = null;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
            fileInputStream = new FileInputStream(fileName);
            Document document = documentBuilder.parse(fileInputStream);
            Element elementRoot = document.getDocumentElement();

            NodeList nodelist_trkpt = elementRoot.getElementsByTagName("trkpt");
            String string = null;
            Line line = new Line();
            line.setDesc(fileName);
            Point point;
            for(int i = 0; i < nodelist_trkpt.getLength(); i++) {
                nTrkPoints++;
                Node node = nodelist_trkpt.item(i);
                NamedNodeMap attributes = node.getAttributes();
                string = attributes.getNamedItem("lat").getTextContent();
                double lat = Double.parseDouble(string);
                string = attributes.getNamedItem("lon").getTextContent();
                double lon = Double.parseDouble(string);
                point = mapCalibration.inverse(lon, lat);
                line.addPoint(point);
            }
            if(line.getNPoints() == 0) {
                SWTUtils.warnMsg("No points found");
            }
            addLine(line);
            line = null;
            ok = true;
        } catch(ParserConfigurationException ex) {
            SWTUtils.excMsg("Parser Configuration Error", ex);
        } catch(FileNotFoundException ex) {
            SWTUtils.errMsg("File not found: " + fileName);
        } catch(SAXException ex) {
            SWTUtils.excMsg("SAX Error", ex);
        } catch(IOException ex) {
            SWTUtils.excMsg("Error reading tract point " + nTrkPoints, ex);
        } finally {
            if(fileInputStream != null) {
                fileInputStream.close();
            }
        }
        return ok;
    }

    /**
     * Gives info about the current Lines.
     * 
     * @return
     */
    public String info() {
        String LS = SWTUtils.LS;
        String info = "";
        // info += this.toString() + LS;
        info += "nLines=" + this.getNLines() + LS;
        Line line;
        for(int i = 0; i < this.getNLines(); i++) {
            line = lines.get(i);
            info += "  Line " + i + " nPoints=" + line.getNPoints() + " " + line
                + LS;
        }
        return info;
    }

    /**
     * @return The value of lines.
     */
    public List<Line> getLines() {
        return lines;
    }

    /**
     * @param lines The new value for lines.
     */
    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

}
