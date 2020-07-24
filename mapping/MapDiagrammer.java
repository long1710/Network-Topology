/*
 * Copyright (c) 2007-2014, Knox College.
 * All rights reserved.
 *
 * This file is part of the PReMAS software package.  For license
 * information see the LICENSE file in the top level directory of the
 * distribution.  
 */

//TODO: description and usage

package mapping;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import simulator.Mesh;
import simulator.MeshLocation;

public class MapDiagrammer {

    Map<Integer, MeshLocation> map;
    Mesh mesh;
    JobDimension dim;
    TaskMapper mppr;

    public MapDiagrammer(String mapType, Scanner scan, int jobX, int jobY, boolean quietMode) {
        super();

        mesh = new Mesh(scan);
        dim = new JobDimension(jobX, jobY, 1);
        MeshLocation[] array = new MeshLocation[jobX * jobY];
        array = mesh.freeProcessors().toArray(array);
        mppr = this.determineMapper(mapType);
        map = mppr.mesh_map(array, dim);

        //below is a copy of the work loop from the paint() method
        //of the Drawing class which is implemented below.
        //Loop added here to keep calculational work (specifically, the
        //calculating of the most-congested edge) out of paint()

        //calculating the space between nodes and the
        //size of the nodes (drawn as circles)
        int xSpace = 590 / mesh.getXDim();
        int ySpace = 590 / mesh.getYDim();
        int xSize = xSpace / 3;
        int ySize = ySpace / 3;

        //to ensure nodes are drawn as circles,
        //the smaller dimension is chosen as the diameter
        int size = xSize;
        if (ySize < xSize)
            size = ySize;

        if(size == 0 && !quietMode)
            return;

        //If quietmode is off, put up the GUI.
        if(!quietMode) {
            Display theWindow = new Display();
        }
    }

    //generates the TaskMapper for the Diagrammer using the
    //mapper argument provided, if any. Defaults to 
    //columnmajor if argument is invalid.
    private TaskMapper determineMapper(String type) {
        if(type.equalsIgnoreCase("columnmajor"))
            return new ColumnMajorTaskMapper();
        else if(type.equalsIgnoreCase("rowmajor"))
            return new RowMajorTaskMapper();
        else if(type.equalsIgnoreCase("corner"))
            return new CornerTaskMapper();
        else if(type.equalsIgnoreCase("allcorners"))
            return new AllCornerTaskMapper();
        else if(type.equalsIgnoreCase("ordered"))
            return new OrderedTaskMapper();
        else if(type.equalsIgnoreCase("preservegrid"))
            return new PreserveGridTaskMapper();
        else if(type.equalsIgnoreCase("twowaypg"))
            return new TwoWayPGTaskMapper();
        else if(type.equalsIgnoreCase("geom"))
        	return new GeometricTaskMapper();
        System.err.println("No match found for mapper type.");
        System.err.println("Defaulting to ColumnMajorTaskMapper.");
        return new ColumnMajorTaskMapper();
    }

    public class Display extends JFrame {

        JLabel meshLabel;
        JLabel jobLabel;
        JLabel statusLabel;
        GridLayout grid;

        public Display() {
            add(new Drawing());
            pack();
            setVisible(true);
        }

        public class Drawing extends JPanel {

            public Drawing() {
                Dimension size = new Dimension(600,630);
                setPreferredSize(size);
                //	    this.setBounds(100, 100, 600, 630);
            }

            //Draws the mapping onto the GUI
            public void paint(Graphics g) {

                //Check to see if job or mesh have a z dimension.
                //If they do, quit.

                if(dim.z > 1) {
                    return;
                }

                if(mesh.getZDim() > 1) {
                    return;
                }

                //calculating the space between nodes and the
                //size of the nodes (drawn as circles)
                int xSpace = 590 / mesh.getXDim();
                int ySpace = 590 / mesh.getYDim();
                int xSize = xSpace / 3;
                int ySize = ySpace / 3;

                //to ensure nodes are drawn as circles,
                //the smaller dimension is chosen as the diameter
                int size = xSize;
                if (ySize < xSize)
                    size = ySize;

                //if the mesh is too big for the GUI, we quit.
                if(size == 0)
                    return;

                //locations map stores the integer designation of job
                //processors mapped to the Points that represent
                //their locations.
                Map<Integer, Point> locations = new HashMap<Integer, Point>();
                //commPattern stores the neighbors for each job processor
                int[][] commPattern = new int[dim.x * dim.y][4];
                //(These are used to draw in the edge lines after the mesh nodes
                //have been added.)

                //giant work loop for generating the mesh node drawing
                //Essentially, for each mesh node, draw it, and if it's
                //a job node, also add its information to locations and
                //commPattern
                for(int j = 0; j < mesh.getYDim(); j++) {
                    for(int i = 0; i < mesh.getXDim(); i++) {
                        Point p = new Point(10 + (i * size * 3), 590 - (j * size * 3));
                        if(mesh.getIsFree(i, j, 0)) {
                            if(map.containsValue(new MeshLocation(i, j, 0))) {
                                for(Map.Entry<Integer, MeshLocation> e : map.entrySet()) {
                                    if(e.getValue().equals(new MeshLocation(i, j, 0))) {
                                        locations.put(e.getKey(), p);
                                        int[] neighbors = mppr.neighbors(e.getKey(), dim);
                                        for(int k = 1; k < 4; k++)
                                            commPattern[e.getKey()][k] = neighbors[k];
                                        g.setColor(Color.black);
                                        g.fillOval(p.x, p.y, size, size);
                                    }
                                }
                            } else {
                                g.setColor(Color.gray);
                                g.fillOval(p.x, p.y, size, size);
                            }
                        } else {
                            g.setColor(Color.lightGray);
                            g.fillOval(p.x, p.y, size, size);
                        }
                    }
                }

                //Now we draw in the edge lines using the locations map and
                //commPattern array. To avoid drawing edges twice, we only 
                //draw edges going upwards and to the right.
                g.setColor(Color.black);
                for(int i = 0; i < (dim.x * dim.y); i++) {
                    Point p1 = locations.get(i);
                    for(int j = 0; j < 4; j++) {
                        if((commPattern[i][j] != -1) && (i < commPattern[i][j])) {
                            Point p2 = locations.get(commPattern[i][j]);
                            int x1 = p1.x;
                            int x2 = p2.x;
                            int y1 = p1.y;
                            int y2 = p2.y;
                            if (j == 1) {
                                //up
                                x1 += size / 2;
                                x2 += size / 2;
                                y2 += size;
                            } else if (j == 3) {
                                //right
                                x1 += size;
                                y1 += size / 2;
                                y2 += size / 2;
                            }
                            g.drawLine(x1, y1, x2, y2);
                        }
                    }
                }

            }
        }
    }

    //Main method, used to set up the Diagrammer and feed it the arguments,
    //then print out the data it generates.
    public static void main(String[] args) throws FileNotFoundException {

        //Check args. Provide list of mappers if args[0] is "-maptypes"
        //and set to quiet mode if args[0] is "-quiet"
        //Other deviations from normal arguments result in a prompt
        //explaing the arguments, after which the program exits.
        boolean quietMode = false;
        if(args.length != 4) {
            if(args.length == 0) {
                System.out.println("Usage: java MapDiagrammer <mapper type> <mesh file> <job x dim> <job y dim>");
                System.out.println("type java MapDiagrammer -maptypes for list of map types");
                System.out.println("Optional mode: -quiet");
                System.out.println("Usage: java MapDiagrammer -quiet <arguments>");
                System.out.println("(This eliminates the visualization and just prints out the data)");
                System.exit(1);
            }
            if(args[0].equals("-maptypes")) {
                System.out.println("Map Types:");
                System.out.println("rowmajor");
                System.out.println("columnmajor");
                System.out.println("corner");
                System.out.println("allcorners");
                System.out.println("ordered");
                System.out.println("preservegrid");
                System.out.println("twowaypg");
                System.out.println("super");
                System.out.println("pgspiral");
                System.exit(0);
            } else if(args[0].equals("-quiet") && args.length == 5) {
                quietMode = true;
            } else {
                System.out.println("Usage: java MapDiagrammer <mapper type> <mesh file> <job x dim> <job y dim>");
                System.out.println("type java MapDiagrammer -maptypes for list of map types");
                System.exit(1);
            }
        }

        //declare and initialize the Diagrammer. Different
        //args indices are used for quiet vs nonquiet mode.
        MapDiagrammer dia;
        if(!quietMode) {
            Scanner scan = new Scanner(new File(args[1]));
            int jobX = Integer.parseInt(args[2]);
            int jobY = Integer.parseInt(args[3]);
            dia = new MapDiagrammer(args[0], scan, jobX, jobY, quietMode);
        } else {
            Scanner scan = new Scanner(new File(args[2]));
            int jobX = Integer.parseInt(args[3]);
            int jobY = Integer.parseInt(args[4]);
            dia = new MapDiagrammer(args[1], scan, jobX, jobY, quietMode);
        }

        //print out data the Diagrammer generates. If in quiet mode,
        //exit immediately afterwards.
        System.out.println("Total Score: " + dia.mppr.score(dia.map, dia.dim));
        System.out.println("Average Job Edge: " + dia.mppr.avgDist(dia.map, dia.dim));
        System.out.println("Longest Job Edge: " + dia.mppr.longestDist(dia.map, dia.dim));
        System.out.println("Highest Edge Usage: " + dia.mppr.highestUsage(dia.map, dia.dim));
        if(quietMode)
            System.exit(0);
    }
}
