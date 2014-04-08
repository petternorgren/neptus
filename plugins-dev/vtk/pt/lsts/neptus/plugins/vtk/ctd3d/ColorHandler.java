/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.ctd3d;

import vtk.vtkDoubleArray;
import vtk.vtkLookupTable;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 *
 */
public class ColorHandler {
    private PointCloudCTD pointcloud;

    private vtkDoubleArray temperatureArray;
    private vtkDoubleArray salinityArray;
    private vtkDoubleArray pressureArray;

    private vtkUnsignedCharArray colorsTemperature;
    private vtkUnsignedCharArray colorsSalinity;
    private vtkUnsignedCharArray colorsPressure;

    private vtkLookupTable lutTemperature;
    private vtkLookupTable lutSalinity;
    private vtkLookupTable lutPressure;

    /**
     * 
     */
    public ColorHandler(PointCloudCTD pointcloud) {
        this.pointcloud = pointcloud;

        this.temperatureArray = pointcloud.getTemperatureArray();
        this.salinityArray = pointcloud.getSalinityArray();
        this.pressureArray = pointcloud.getPressureArray();

        this.setColorsTemperature(new vtkUnsignedCharArray());
        this.setColorsSalinity(new vtkUnsignedCharArray());
        this.setColorsPressure(new vtkUnsignedCharArray());

        this.lutTemperature = new vtkLookupTable();
        this.lutSalinity = new vtkLookupTable();
        this.lutPressure = new vtkLookupTable();
    }

    public void gerenerateColors() {
        vtkLookupTable look1 = new vtkLookupTable();
        look1.SetRange(temperatureArray.GetRange());
        look1.SetScaleToLinear();
        look1.Build();

        lutTemperature.SetRange(temperatureArray.GetRange());
        for (int i = 1; i <= 256 ; ++i) {
            lutTemperature.SetTableValue(256 - i, look1.GetTableValue(i));
        }
        lutTemperature.Build();

        vtkLookupTable look2 = new vtkLookupTable();
        look2.SetRange(salinityArray.GetRange());
        look2.SetScaleToLinear();
        look2.Build();

        lutSalinity.SetRange(salinityArray.GetRange());
        for (int i = 1; i <= 256; ++i) {
            lutSalinity.SetTableValue(256 - i, look2.GetTableValue(i));
        }
        lutSalinity.Build();

        vtkLookupTable look3 = new vtkLookupTable();
        look3.SetRange(pressureArray.GetRange());
        look3.SetScaleToLinear();
        look3.Build();

        lutPressure.SetRange(pressureArray.GetRange());
        for(int i = 1; i <= 256; ++i) {
            lutPressure.SetTableValue(256 - i, look3.GetTableValue(i));
        }
        lutPressure.Build();

        getColorsTemperature().SetNumberOfComponents(3);
        getColorsTemperature().SetName("colorsTemp");
        getColorsSalinity().SetNumberOfComponents(3);
        getColorsSalinity().SetName("colorsSalinity");
        getColorsPressure().SetNumberOfComponents(3);
        getColorsPressure().SetName("colorsPressure");

        for (int i = 0; i < pointcloud.getNumberOfPoints(); ++i) {
            double temperature = temperatureArray.GetValue(i);
            double salinity = salinityArray.GetValue(i);
            double pressure = pressureArray.GetValue(i);

            double[] tempColor = new double[3];
            double[] salColor = new double[3];
            double[] pressColor = new double[3];

            lutTemperature.GetColor(temperature, tempColor);
            lutSalinity.GetColor(salinity, salColor);
            lutPressure.GetColor(pressure, pressColor);

            char[] colorTemp = new char[3];
            char[] colorSalinity = new char[3];
            char[] colorPressure = new char[3];

            for (int j = 0; j < 3; ++j) {
                colorTemp[j] = (char) (255.0 * tempColor[j]);
                colorSalinity[j] = (char) (255.0 * salColor[j]);
                colorPressure[j] = (char) (255.0 * pressColor[j]);
            }

            getColorsTemperature().InsertNextTuple3(colorTemp[0], colorTemp[1], colorTemp[2]);
            getColorsSalinity().InsertNextTuple3(colorSalinity[0], colorSalinity[1], colorSalinity[2]);
            getColorsPressure().InsertNextTuple3(colorPressure[0], colorPressure[1], colorPressure[2]);
        }
    }

    /**
     * @return the lutTemperature
     */
    public vtkLookupTable getLutTemperature() {
        return lutTemperature;
    }

    /**
     * @param lutTemperature the lutTemperature to set
     */
    public void setLutTemperature(vtkLookupTable lutTemperature) {
        this.lutTemperature = lutTemperature;
    }

    /**
     * @return the lutSalinity
     */
    public vtkLookupTable getLutSalinity() {
        return lutSalinity;
    }

    /**
     * @param lutSalinity the lutSalinity to set
     */
    public void setLutSalinity(vtkLookupTable lutSalinity) {
        this.lutSalinity = lutSalinity;
    }

    /**
     * @return the lutPressure
     */
    public vtkLookupTable getLutPressure() {
        return lutPressure;
    }

    /**
     * @param lutPressure the lutPressure to set
     */
    public void setLutPressure(vtkLookupTable lutPressure) {
        this.lutPressure = lutPressure;
    }

    /**
     * @return the colorsTemperature
     */
    public vtkUnsignedCharArray getColorsTemperature() {
        return colorsTemperature;
    }

    /**
     * @param colorsTemperature the colorsTemperature to set
     */
    private void setColorsTemperature(vtkUnsignedCharArray colorsTemperature) {
        this.colorsTemperature = colorsTemperature;
    }

    /**
     * @return the colorsSalinity
     */
    public vtkUnsignedCharArray getColorsSalinity() {
        return colorsSalinity;
    }

    /**
     * @param colorsSalinity the colorsSalinity to set
     */
    private void setColorsSalinity(vtkUnsignedCharArray colorsSalinity) {
        this.colorsSalinity = colorsSalinity;
    }

    /**
     * @return the colorsPressure
     */
    public vtkUnsignedCharArray getColorsPressure() {
        return colorsPressure;
    }

    /**
     * @param colorsPressure the colorsPressure to set
     */
    private void setColorsPressure(vtkUnsignedCharArray colorsPressure) {
        this.colorsPressure = colorsPressure;
    }
}