package net.kenevans.maplines.lines;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import net.kenevans.maplines.utils.SWTUtils;

public class MapCalibration
{
    private List<MapData> dataList = new ArrayList<MapData>();
    private MapTransform transform;

    public boolean read(File file) throws NumberFormatException, IOException {
        MapData data = null;
        boolean ok = false;
        BufferedReader in = null;
        String[] tokens = null;
        int x, y;
        double lon, lat;
        in = new BufferedReader(new FileReader(file));
        String line;
        while((line = in.readLine()) != null) {
            tokens = line.trim().split("\\s+");
            // Skip blank lines
            if(tokens.length == 0) {
                continue;
            }
            // Skip lines starting with #
            if(tokens[0].trim().startsWith("#")) {
                continue;
            }
            x = Integer.parseInt(tokens[0]);
            y = Integer.parseInt(tokens[1]);
            lon = Double.parseDouble(tokens[2]);
            lat = Double.parseDouble(tokens[3]);
            // DEBUG
            // System.out.println(String.format("x=%d y=%d lon=%.6f lat=%.6f",
            // x,
            // y, lon, lat));
            data = new MapData(x, y, lon, lat);
            dataList.add(data);
        }
        in.close();
        // Make the transform
        // createTransform3();
        createTransform();
        return ok;
    }

    /**
     * Calculates a, b, c, d, e, and f using singular value decomposition.
     */
    protected void createTransform() {
        transform = null;
        if(dataList.size() < 3) {
            SWTUtils.errMsg("Need at least three data points for calibration.");
            return;
        }

        // Define the matrices
        int nPoints2 = 2 * dataList.size();
        Matrix aa = new Matrix(nPoints2, 6);
        Matrix bb = new Matrix(nPoints2, 1);
        MapData data = null;
        int row;
        for(int i = 0; i < dataList.size(); i++) {
            data = dataList.get(i);
            row = 2 * i;
            aa.set(row, 0, data.getX());
            aa.set(row, 1, data.getY());
            aa.set(row, 4, 1);
            bb.set(row, 0, data.getLon());
            row++;
            aa.set(row, 2, data.getX());
            aa.set(row, 3, data.getY());
            aa.set(row, 5, 1);
            bb.set(row, 0, data.getLat());
        }

        // Get the singular values
        try {
            SingularValueDecomposition svd = new SingularValueDecomposition(aa);
            Matrix u = svd.getU();
            Matrix v = svd.getV();
            Matrix wi = svd.getS();
            for(int i = 0; i < 6; i++) {
                wi.set(i, i, 1. / wi.get(i, i));
            }
            Matrix aainv = v.times(wi).times(u.transpose());
            Matrix xx = aainv.times(bb);

            // // DEBUG
            // System.out.println("u = ");
            // printMatrix(u);
            // System.out.println("s = ");
            // printMatrix(svd.getS());
            // System.out.println("v = ");
            // printMatrix(v);
            // System.out.println("aa = ");
            // printMatrix(aa);
            // Matrix aa1 = u.times(wi).times(v.transpose());
            // System.out.println("aa1 = ");
            // printMatrix(aa1);
            // System.out.println("wi = ");
            // printMatrix(wi);
            // System.out.println("aainv = ");
            // printMatrix(aainv);
            // System.out.println("xx = ");
            // printMatrix(xx);
            // Matrix test = aa.times(aainv);
            // System.out.println("aa.aainv = ");
            // printMatrix(test);

            double a = xx.get(0, 0);
            double b = xx.get(1, 0);
            double c = xx.get(2, 0);
            double d = xx.get(3, 0);
            double e = xx.get(4, 0);
            double f = xx.get(5, 0);
            transform = new MapTransform(a, b, c, d, e, f);
            // DEBUG
            // System.out.println(String.format(
            // " a=%.3g b=%.3g c=%.3g d=%.3g e=%.3g f= %.3g",
            // transform.getA(), transform.getB(), transform.getC(),
            // transform.getD(), transform.getE(), transform.getF()));
        } catch(Exception ex) {
            SWTUtils.excMsg("Failed to create calibration transform", ex);
            transform = null;
        }
    }

    /**
     * Calculates a, b, c, d, e, and f using 3 calibration points. Using more
     * than 3 points is over-determined. The best solution would be to use
     * singular value decomposition.
     */
    protected void createTransform3() {
        transform = null;
        int x1, x2, x3, y1, y2, y3;
        double lon1, lon2, lon3, lat1, lat2, lat3;
        if(dataList.size() < 3) {
            SWTUtils.errMsg("Need at least three data points for calibration.");
            return;
        }
        if(dataList.size() > 3) {
            SWTUtils.errMsg("Using first three data points for calibration.");
            return;
        }
        try {
            x1 = dataList.get(0).getX();
            y1 = dataList.get(0).getY();
            lon1 = dataList.get(0).getLon();
            lat1 = dataList.get(0).getLat();
            x2 = dataList.get(1).getX();
            y2 = dataList.get(1).getY();
            lon2 = dataList.get(1).getLon();
            lat2 = dataList.get(1).getLat();
            x3 = dataList.get(2).getX();
            y3 = dataList.get(2).getY();
            lon3 = dataList.get(2).getLon();
            lat3 = dataList.get(2).getLat();

            double a = ((lon2 - lon1) * y3 + (lon1 - lon3) * y2
                + (lon3 - lon2) * y1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            double b = -((lon2 - lon1) * x3 + (lon1 - lon3) * x2
                + (lon3 - lon2) * x1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            double c = ((lat2 - lat1) * y3 + (lat1 - lat3) * y2
                + (lat3 - lat2) * y1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            double d = -((lat2 - lat1) * x3 + (lat1 - lat3) * x2
                + (lat3 - lat2) * x1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            double e = ((lon1 * x2 - lon2 * x1) * y3
                + (lon3 * x1 - lon1 * x3) * y2 + (lon2 * x3 - lon3 * x2) * y1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            double f = ((lat1 * x2 - lat2 * x1) * y3
                + (lat3 * x1 - lat1 * x3) * y2 + (lat2 * x3 - lat3 * x2) * y1)
                / ((x2 - x1) * y3 + (x1 - x3) * y2 + (x3 - x2) * y1);
            transform = new MapTransform(a, b, c, d, e, f);
            // DEBUG
            // System.out.println(String.format(
            // " a=%.3g b=%.3g c=%.3g d=%.3g e=%.3g f= %.3g",
            // transform.getA(), transform.getB(), transform.getC(),
            // transform.getD(), transform.getE(), transform.getF()));
        } catch(Exception ex) {
            SWTUtils.excMsg("Failed to create calibration transform", ex);
            transform = null;
        }
    }

    /**
     * Transforms the pixel coordinates (x,y) to (longitude, latitude).
     * 
     * @param x
     * @param y
     * @return {longitude, latitude}
     */
    public double[] transform(int x, int y) {
        if(transform == null) {
            return null;
        }
        double[] val = new double[2];
        val[0] = transform.getA() * x + transform.getB() * y + transform.getE();
        val[1] = transform.getC() * x + transform.getD() * y + transform.getF();
        // DEBUG
        // System.out.println(
        // String.format("x=%d y=%d lon=%.6f lat=%.6f", x, y, val[0], val[1]));
        return val;
    }

    public Point inverse(double lon, double lat) {
        if(transform == null) {
            return null;
        }
        double det = transform.getA() * transform.getD()
            - transform.getB() * transform.getC();
        if(det == 0) {
            return null;
        }
        // DEBUG
        // System.out.printf("%10.6f %10.6f %10.6f %10.6f %10.6f %10.6f",
        // transform.getA(), transform.getB(), transform.getC(),
        // transform.getD(), transform.getE(), transform.getF());
        double v1, v2;
        lon -= transform.getE();
        lat -= transform.getF();
        v1 = transform.getD() * lon - transform.getB() * lat;
        v2 = transform.getA() * lat - transform.getC() * lon;
        v1 /= det;
        v2 /= det;
        // DEBUG
        // System.out.printf("%10.6f %10.6f %6d %6d" + SWTUtils.LS, lon, lat,
        // (int)(v1 + .5), (int)(v2 + .5));
        return new Point((int)(v1 + .5), (int)(v2 + .5));
    }

    /**
     * Simple printout of a Matrix.
     * 
     * @param m
     */
    static public void printMatrix(Matrix m) {
        double[][] d = m.getArray();

        for(int row = 0; row < d.length; row++) {
            for(int col = 0; col < d[row].length; col++) {
                System.out.printf("%6.4f\t", m.get(row, col));
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * @return The value of transform.
     */
    public MapTransform getTransform() {
        return transform;
    }

    /**
     * @return The value of dataList.
     */
    public List<MapData> getDataList() {
        return dataList;
    }

    public class MapTransform
    {
        private double a;
        private double b;
        private double c;
        private double d;
        private double e;
        private double f;

        public MapTransform(double a, double b, double c, double d, double e,
            double f) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
        }

        public double getA() {
            return a;
        }

        public void setA(double a) {
            this.a = a;
        }

        public double getB() {
            return b;
        }

        public void setB(double b) {
            this.b = b;
        }

        public double getC() {
            return c;
        }

        public void setC(double c) {
            this.c = c;
        }

        public double getD() {
            return d;
        }

        public void setD(double d) {
            this.d = d;
        }

        public double getE() {
            return e;
        }

        public void setE(double e) {
            this.e = e;
        }

        public double getF() {
            return f;
        }

        public void setF(double f) {
            this.f = f;
        }

    }

    public class MapData
    {
        private int x;
        private int y;
        private double lon;
        private double lat;

        public MapData(int x, int y, double lon, double lat) {
            this.x = x;
            this.y = y;
            this.lon = lon;
            this.lat = lat;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

    }

}
