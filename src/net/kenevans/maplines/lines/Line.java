package net.kenevans.maplines.lines;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import net.kenevans.maplines.utils.SWTUtils;

/*
 * Created on Aug 17, 2013
 * By Kenneth Evans, Jr.
 */

public class Line
{
    public static final RGB DEFAULT_LINE_COLOR = new RGB(255, 0, 0);
    private RGB rgb = DEFAULT_LINE_COLOR;
    private List<Point> points = new ArrayList<Point>();
    private String desc = "";
    private boolean selected = false;

    public Line(int[][] data) {
        if(data == null || data.length == 0) {
            return;
        }
        if(data[0].length != 2) {
            SWTUtils.errMsgAsync(null,
                "Data array must be an array of sets of two integers");
            return;
        }
        for(int[] point : data) {
            points.add(new Point(point[0], point[1]));
        }
    }

    public Line() {
        // Do nothing
    }

    public Line(RGB color) {
        this.rgb = color;
    }

    public void addPoint(Point point) {
        if(points != null) {
            points.add(point);
        }
    }

    public void removePoint(Point point) {
        if(points != null) {
            points.remove(point);
        }
    }

    public void removePoint(int index) {
        if(points != null) {
            points.remove(index);
        }
    }

    /**
     * Gets the number of points in the line.
     * 
     * @return
     */
    public int getNPoints() {
        return points.size();
    }

    /**
     * Deletes the last point of the line if there is one.
     */
    public void deleteLastPoint() {
        int nPoints = points.size();
        if(nPoints > 0) {
            points.remove(nPoints - 1);
        }
    }

    /**
     * @return The value of rgb.
     */
    public RGB getRgb() {
        return rgb;
    }

    /**
     * @param rgb The new value for rgb.
     */
    public void setRgb(RGB rgb) {
        this.rgb = rgb;
    }

    /**
     * @return The value of points.
     */
    public List<Point> getPoints() {
        return points;
    }

    /**
     * @param points The new value for points.
     */
    public void setPoints(List<Point> points) {
        this.points = points;
    }

    /**
     * @return The value of desc.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @param desc The new value for desc.
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * @return The value of selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected The new value for selected.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
