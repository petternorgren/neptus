/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 11/06/2016
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Systems Interaction", icon = "images/imc.png", description = "This will allow to select a system in the renderer and display extra info.")
@LayerPriority(priority = 120)
public class SystemsInteraction extends ConsoleInteraction {

    private static final int PIXEL_DISTANCE_TO_SELECT = 5;
    private static final int RECT_WIDTH = 228;
    private static final int RECT_HEIGHT = 85;
    private static final int MARGIN = 5;
    
    @NeptusProperty(name = "Consider External Systems Icons", userLevel = LEVEL.REGULAR)
    public boolean considerExternalSystemsIcons = true;
    @NeptusProperty(name = "Minutes To Consider Systems Without Known Location", userLevel = LEVEL.REGULAR)
    public int minutesToConsiderSystemsWithoutKnownLocation = 5;
    @NeptusProperty(name = "Minutes to Show Distress Signal", category = "Test", userLevel = LEVEL.ADVANCED)
    private int minutesToShowDistress = 5; 

    private short counterShow = 0;
    private ArrayList<ImcSystem> imcSystems = new ArrayList<>();
    private ArrayList<ExternalSystem> extSystems = new ArrayList<>();
    
    private JLabel labelToPaint = new JLabel();
    
    public SystemsInteraction() {
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#initInteraction()
     */
    @Override
    public void initInteraction() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#cleanInteraction()
     */
    @Override
    public void cleanInteraction() {
        synchronized (imcSystems) {
            imcSystems.clear();
            extSystems.clear();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#paintInteraction(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D renderer) {
        String txt = collectTextToPaint();
        
        if (txt == null || txt.isEmpty())
            return;
        
        labelToPaint.setText(txt);
        labelToPaint.setForeground(Color.BLACK);
        labelToPaint.setVerticalAlignment(JLabel.NORTH);
        labelToPaint.setHorizontalTextPosition(JLabel.CENTER);
        labelToPaint.setHorizontalAlignment(JLabel.LEFT);
        labelToPaint.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setTransform(renderer.getIdentity());
        
        int width = RECT_WIDTH;
        int height = RECT_HEIGHT;
        height = (int) Math.max(height, labelToPaint.getPreferredSize().getHeight());
        
        // Pull up for lat/lon label
        g2.translate(0, renderer.getHeight() - (height + MARGIN));
        g2.translate(0, -40);

        g2.setColor(new Color(0, 0, 0, 200));

        g2.drawRoundRect(MARGIN, MARGIN, width, height, 20, 20);

        g2.setColor(new Color(255, 255, 255, 230));

        g2.fillRoundRect(MARGIN, MARGIN, width, height, 20, 20);

        g2.translate(2.5, 2.5);
        labelToPaint.setBounds(0, 0, width, height);
        labelToPaint.paint(g2);
        
        g2.translate(-2.5, -2.5);
        String txtDistress = collectTextDistressToPaint();
        if (!txtDistress.isEmpty()) {
            labelToPaint.setText(txtDistress);
            
            int widthD = RECT_WIDTH;
            int heightD = (int) labelToPaint.getPreferredSize().getHeight() + MARGIN * 2;
            
            g2.translate(0, -(heightD + MARGIN));

            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawRoundRect(MARGIN, MARGIN, widthD, heightD, 20, 20);

            g2.setColor(new Color(255, 155, 155, 150));
            g2.fillRoundRect(MARGIN, MARGIN, widthD, heightD, 20, 20);

            g2.translate(2.5, 2.5);
            labelToPaint.setBounds(0, 0, widthD, heightD);
            labelToPaint.paint(g2);
            
            LocationType loc = collectLocation();
            if (loc != null) {
                Graphics2D g3 = (Graphics2D) g.create();
                Point2D pt = renderer.getScreenPosition(loc);
                g3.translate(-renderer.getWidth() / 2., renderer.getHeight() / 2.);
                g3.translate(pt.getX(), -pt.getY());
                g3.setStroke(new BasicStroke(3));
                g3.setColor(new Color(255, 155, 155, 255));
                int s = 40;
                g3.drawOval(-s / 2, -s / 2, s, s);
                s = 60;
                g3.drawOval(-s / 2, -s / 2, s, s);
                s = 80;
                g3.drawOval(-s / 2, -s / 2, s, s);
                g3.dispose();
            }
        }
        
        g2.dispose();
    }
    
    /**
     * @return
     */
    private String collectTextToPaint() {
        String ret = null;
        
        synchronized (this.imcSystems) {
            int allCount = imcSystems.size() + extSystems.size();
            if (allCount > 0) {
                StringBuilder sb = new StringBuilder("<html>");
                int idx = counterShow % allCount;
                if (idx < imcSystems.size()) {
                    ImcSystem sys = imcSystems.get(idx);
                    sb.append("<font color=\"").append(String.format("#%02X%02X%02X", 28, 37, 58)).append("\">");
                    sb.append("<b>").append(sys.getName().toUpperCase()).append("</b>"); //.append("<font size=\"2\">").append("</font>")
                    sb.append("</font>");
                    
                    sb.append("<font size=\"2\">");

                    sb.append("<br/>").append("<b>").append(I18n.text("Type")).append(": ").append("</b>")
                        .append(sys.getType() == SystemTypeEnum.VEHICLE ? sys.getTypeVehicle() : sys.getType());
                
                    sb.append("<br/>").append("<b>").append("IMC: ").append("</b>").append(sys.getId().toPrettyString().toUpperCase());
                    
                    Object speed = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
                    Object course = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
                    Object rateOfturn = sys.retrieveData(SystemUtils.RATE_OF_TURN_DEGS_PER_MIN_KEY);
                    sb.append("<br/>").append("<b>").append(I18n.text("Speed")).append("/").append(I18n.text("Course"));
                    if (rateOfturn != null)
                        sb.append("/").append(I18n.textc("ROT", "Rate of Turn"));
                    sb.append(": ").append("</b>");
                    sb.append(speed == null ? "- " : MathMiscUtils.round(((Number) speed).doubleValue(), 1)).append("m/s");
                    sb.append("<b> | </b>");
                    sb.append(course == null ? "- " : (int) MathMiscUtils.round(((Number) course).doubleValue(), 0)).append("\u00B0");
                    if (rateOfturn != null) {
                        sb.append("<b> | </b>");
                        sb.append(rateOfturn == null ? "- " : (int) MathMiscUtils.round(((Number) rateOfturn).doubleValue(), 0)).append("\u00B0/min");
                    }
                    Object draught = sys.retrieveData(SystemUtils.DRAUGHT_KEY);
                    sb.append("<br/>").append("<b>").append(I18n.text("Draught")).append(": ").append("</b>")
                            .append(draught == null ? "- " : MathMiscUtils.round(((Number) draught).doubleValue(), 1)).append("m");
                    
                    ConsoleSystem consoleSys = getConsole().getSystem(sys.getName());
                    String navStatus = "-";
                    if (consoleSys != null)
                        navStatus = consoleSys.getVehicleState().toString();
                    sb.append("<br/>").append("<b>").append(I18n.text("Status")).append(": ").append("</b>")
                        .append(navStatus);

                    sb.append("</font>");
                }
                else {
                    ExternalSystem sys = extSystems.get(idx - imcSystems.size());
                    sb.append("<font color=\"").append(String.format("#%02X%02X%02X", 28, 37, 58)).append("\">");
                    sb.append("<b>").append(sys.getName()).append("</b>");
                    sb.append("</font>");

                    sb.append("<font size=\"2\">");

                    Object shipType = sys.retrieveData(SystemUtils.SHIP_TYPE_KEY);
                    sb.append("<br/>").append("<b>").append(I18n.text("Type")).append(": ").append("</b>")
                        .append(shipType != null ? shipType : sys.getTypeExternal());

                    Object mmsi = sys.retrieveData(SystemUtils.MMSI_KEY);
                    Object callSign = sys.retrieveData(SystemUtils.CALL_SIGN_KEY);
                    if (mmsi != null || callSign != null) {
                        sb.append("<br/>");
                        if (mmsi != null)
                            sb.append("<b>").append("MMSI: ").append("</b>").append(mmsi);
                        if (callSign != null)
                            sb.append(" ").append("<b>").append(I18n.text("Call-Sign")).append(": ").append("</b>").append(callSign);

                        Object speed = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
                        Object course = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
                        Object rateOfturn = sys.retrieveData(SystemUtils.RATE_OF_TURN_DEGS_PER_MIN_KEY);
                        sb.append("<br/>").append("<b>").append(I18n.text("Speed")).append("/").append(I18n.text("Course"));
                        if (rateOfturn != null)
                            sb.append("/").append(I18n.textc("ROT", "Rate of Turn"));
                        sb.append(": ").append("</b>");
                        sb.append(speed == null ? "- " : MathMiscUtils.round(((Number) speed).doubleValue(), 1)).append("m/s");
                        sb.append("<b> | </b>");
                        sb.append(course == null ? "- " : (int) MathMiscUtils.round(((Number) course).doubleValue(), 0)).append("\u00B0");
                        if (rateOfturn != null) {
                            sb.append("<b> | </b>");
                            sb.append(rateOfturn == null ? "- " : (int) MathMiscUtils.round(((Number) rateOfturn).doubleValue(), 0)).append("\u00B0/min");
                        }

                        Object draught = sys.retrieveData(SystemUtils.DRAUGHT_KEY);
                        sb.append("<br/>").append("<b>").append(I18n.text("Draught")).append(": ").append("</b>")
                            .append(draught == null ? "- " : MathMiscUtils.round(((Number) draught).doubleValue(), 1)).append("m");
                        Object width = sys.retrieveData(SystemUtils.WIDTH_KEY);
                        Object lenght = sys.retrieveData(SystemUtils.LENGHT_KEY);
                        sb.append(" ").append("<b>").append(I18n.textc("Size W/L", "Size width/lenght")).append(": ").append("</b>")
                            .append(width == null ? "- " : (int) MathMiscUtils.round(((Number) width).doubleValue(), 0)).append("m")
                            .append("<b> | </b>")
                            .append(lenght == null ? "- " : (int) MathMiscUtils.round(((Number) lenght).doubleValue(), 0)).append("m");
                        
                        Object navStatus = sys.retrieveData(SystemUtils.NAV_STATUS_KEY);
                        sb.append("<br/>").append("<b>").append(I18n.text("Status")).append(": ").append("</b>")
                            .append(navStatus != null ? navStatus : "-");
                    }

                    sb.append("</font>");
                }
                
                sb.append("</html>");

                ret = sb.toString();
                return ret;
            }
        }
        
        return null;
    }

    /**
     * @return
     */
    private String collectTextDistressToPaint() {
        String ret = null;
        Object distress = null;
        long distressTimeMillis = -1;
        LocationType loc = null;

        synchronized (this.imcSystems) {
            int allCount = imcSystems.size() + extSystems.size();
            if (allCount > 0) {
                int idx = counterShow % allCount;
                if (idx < imcSystems.size()) {
                    ImcSystem sys = imcSystems.get(idx);
                    if (sys != null) {
                        distress = sys.retrieveData(SystemUtils.DISTRESS_MSG_KEY,
                                minutesToShowDistress * DateTimeUtil.MINUTE);
                        distressTimeMillis = sys.retrieveDataTimeMillis(SystemUtils.DISTRESS_MSG_KEY);
                        loc = sys.getLocation();
                    }
                }
                else {
                    ExternalSystem sys = extSystems.get(idx - imcSystems.size());
                    if (sys != null) {
                        distress = sys.retrieveData(SystemUtils.DISTRESS_MSG_KEY,
                                minutesToShowDistress * DateTimeUtil.MINUTE);
                        distressTimeMillis = sys.retrieveDataTimeMillis(SystemUtils.DISTRESS_MSG_KEY);
                        loc = sys.getLocation();
                    }
                }
            }
        }

//        distress = "DISTRESS :: Oxygen=15.4,Battery=23.2,People=76";

        if (distress == null)
            return "";
        
        String distressStr = "" + distress;
        distressStr = distressStr.replaceFirst("DISTRESS", "");
        distressStr = distressStr.replaceFirst(" :: ", "");
        if (loc != null)
            distressStr += " @depth" + Math.round(loc.getDepth()) + "m";
        
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<font color=\"").append(String.format("#%02X%02X%02X", 228, 37, 58)).append("\">");
        sb.append("<b>").append("&gt;&gt;&gt; DISTRESS &lt;&lt;&lt;").append("</b>");
        sb.append("</font>");
        sb.append("<font size=\"2\">");
        if (distressTimeMillis > 0) {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;\u2206t ");
            sb.append(DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - distressTimeMillis, true));
        }
        sb.append("<br/>");
        sb.append(distressStr);
        sb.append("</font>");
        sb.append("</html>");

        ret = sb.toString();
        return ret;
    }
    
    private LocationType collectLocation() {
        int allCount = imcSystems.size() + extSystems.size();
        LocationType loc = null;
        if (allCount > 0) {
            int idx = counterShow % allCount;
            if (idx < imcSystems.size()) {
                ImcSystem sys = imcSystems.get(idx);
                loc = sys.getLocation();
            }
            else {
                ExternalSystem sys = extSystems.get(idx - imcSystems.size());
                loc = sys.getLocation();
            }
            
            return loc;
        }
        
        return null;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#mouseMoved(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        super.mouseMoved(event, source);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleInteraction#mouseClicked(java.awt.event.MouseEvent, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        super.mouseClicked(event, source);
        
        if (!SwingUtilities.isLeftMouseButton(event))
            return;
        
        counterShow++;
        
        ArrayList<ImcSystem> imcSystems = new ArrayList<>();
        ArrayList<ExternalSystem> extSystems = new ArrayList<>();
        
        for (ImcSystem sys : ImcSystemsHolder.lookupAllSystems()) {
            if (System.currentTimeMillis() - sys.getLocationTimeMillis() > minutesToConsiderSystemsWithoutKnownLocation
                    * DateTimeUtil.MINUTE)
                continue;
            LocationType loc = sys.getLocation();
            Point2D locScreenXY = source.getScreenPosition(loc);
            double dist = locScreenXY.distance(event.getPoint());
            if (dist <= PIXEL_DISTANCE_TO_SELECT)
                imcSystems.add(sys);
        }
        
        if (considerExternalSystemsIcons) {
            for (ExternalSystem sys : ExternalSystemsHolder.lookupAllSystems()) {
                if (System.currentTimeMillis()
                        - sys.getLocationTimeMillis() > minutesToConsiderSystemsWithoutKnownLocation
                                * DateTimeUtil.MINUTE)
                    continue;
                LocationType loc = sys.getLocation();
                Point2D locScreenXY = source.getScreenPosition(loc);
                double dist = locScreenXY.distance(event.getPoint());
                if (dist <= PIXEL_DISTANCE_TO_SELECT)
                    extSystems.add(sys);
            }
        }
        
        synchronized (this.imcSystems) {
            this.imcSystems.clear();
            this.imcSystems.addAll(imcSystems);
            this.extSystems.clear();
            this.extSystems.addAll(extSystems);
        }
        
//        System.out.println(" >> " + imcSystems.stream().map(ImcSystem::getName).collect(Collectors.joining())
//                + " :: " + extSystems.stream().map(ExternalSystem::getId).collect(Collectors.joining()));
    }
}
