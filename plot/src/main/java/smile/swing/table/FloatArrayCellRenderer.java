/*******************************************************************************
 * Copyright (c) 2010-2019 Haifeng Li
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package smile.swing.table;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * Float array renderer in JTable.
 * 
 * @author Haifeng Li
 */
@SuppressWarnings("serial")
public class FloatArrayCellRenderer extends DefaultTableCellRenderer {

    /**
     * Constructor.
     */
    public FloatArrayCellRenderer() {
    }
    
    @Override
    public void setValue(Object value) {
        if (value == null) {
            setText("[]");
            return;
        }
        
        float[] data = (float[]) value;
        
        StringBuilder builder = new StringBuilder();
        if (data.length > 0) {
            builder.append("[").append(data[0]);
        }
        
        for (int i = 1; i < data.length; i++) {
            builder.append(", ").append(data[i]);
        }
        builder.append("]");
        setText(builder.toString());
    }
}