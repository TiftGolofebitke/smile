/*
 * Copyright (c) 2010-2021 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 */
package smile.plot.vega;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Single view specification, which describes a view that uses a single
 * mark type to visualize the data.
 *
 * @author Haifeng Li
 */
public class View extends VegaLite {
    /**
     * Constructor.
     */
    public View() {
    }

    /**
     * Sets the width of a plot with a continuous x-field,
     * or the width per discrete step of a discrete x-field or no x-field.
     */
    public View width(int width) {
        spec.put("width", width);
        return this;
    }

    /**
     * Sets the height of a plot with a continuous y-field,
     * or the height per discrete step of a discrete y-field or no y-field.
     */
    public View height(int height) {
        spec.put("height", height);
        return this;
    }

    /**
     * To enable responsive sizing on width.
     * @param width it should be set to "container".
     */
    public View width(String width) {
        assert width == "container" : "Invalid width: " + width;
        spec.put("width", "container");
        return this;
    }

    /**
     * To enable responsive sizing on height.
     * @param height it should be set to "container".
     */
    public View height(String height) {
        assert height == "container" : "Invalid height: " + height;
        spec.put("height", "container");
        return this;
    }

    /**
     * For a discrete x-field, sets the width per discrete step.
     */
    public View widthStep(int step) {
        ObjectNode width = mapper.createObjectNode();
        width.put("step", step);
        spec.set("width", width);
        return this;
    }

    /**
     * For a discrete y-field, sets the height per discrete step..
     */
    public View heightStep(int step) {
        ObjectNode height = mapper.createObjectNode();
        height.put("step", step);
        spec.set("height", height);
        return this;
    }

    /**
     * Returns the mark definition object.
     * @param type The mark type. This could a primitive mark type (one of
     *            "bar", "circle", "square", "tick", "line", "area", "point",
     *            "geoshape", "rule", and "text") or a composite mark type
     *            ("boxplot", "errorband", "errorbar").
     * @return the mark definition object.
     */
    public Mark mark(String type) {
        ObjectNode node = spec.putObject("mark");
        node.put("type", type);
        return new Mark(node);
    }

    /** Returns the encoding object. */
    private ObjectNode encoding() {
        return spec.has("encoding") ? (ObjectNode) spec.get("encoding") : spec.putObject("encoding");
    }

    /**
     * Returns the field object for encoding a channel.
     * @param channel Vega-Lite supports the following groups of encoding channels:
     *   - Position Channels: x, y, x2, y2, xError, yError, xError2, yError2
     *   - Position Offset Channels: xOffset, yOffset
     *   - Polar Position Channels: theta, theta2, radius, radius2
     *   - Geographic Position Channels: longitude, latitude, longitude2, latitude2
     *   - Mark Property Channels: angle, color (and fill / stroke), opacity, fillOpacity, strokeOpacity, shape, size, strokeDash, strokeWidth
     *   - Text and Tooltip Channels: text, tooltip
     *   - Hyperlink Channel: href
     *   - Description Channel: description
     *   - Level of Detail Channel: detail
     *   - Key Channel: key
     *   - Order Channel: order
     *   - Facet Channels: facet, row, column
     * @param field A string defining the name of the field from which to pull
     *             a data value or an object defining iterated values from the
     *             repeat operator.
     * @return the field object for encoding the channel.
     */
    public Field encoding(String channel, String field) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("field", field);
        return new Field(node);
    }

    /**
     * Sets an encoded constant visual value.
     * @param channel the encoding channel.
     * @param value the constant visual value.
     * @return this object.
     */
    public View encodingValue(String channel, int value) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("value", value);
        return this;
    }

    /**
     * Sets an encoded constant visual value.
     * @param channel the encoding channel.
     * @param value the constant visual value.
     * @return this object.
     */
    public View encodingValue(String channel, double value) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("value", value);
        return this;
    }

    /**
     * Sets an encoded constant visual value.
     * @param channel the encoding channel.
     * @param value the constant visual value.
     * @return this object.
     */
    public View encodingValue(String channel, String value) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("value", value);
        return this;
    }

    /**
     * Sets a constant data value encoded via a scale.
     * @param channel the encoding channel.
     * @param datum the constant data value.
     * @return this object.
     */
    public View encodingDatum(String channel, int datum) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("datum", datum);
        return this;
    }

    /**
     * Sets a constant data value encoded via a scale.
     * @param channel the encoding channel.
     * @param datum the constant data value.
     * @return this object.
     */
    public View encodingDatum(String channel, double datum) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("datum", datum);
        return this;
    }

    /**
     * Sets a constant data value encoded via a scale.
     * @param channel the encoding channel.
     * @param datum the constant data value.
     * @return this object.
     */
    public View encodingDatum(String channel, String datum) {
        ObjectNode encoding = encoding();
        ObjectNode node = encoding.putObject(channel);
        node.put("datum", datum);
        return this;
    }

    /** Sets a mark property by value. */
    public View setPropertyValue(String prop, JsonNode value) {
        ObjectNode data = mapper.createObjectNode();
        data.set("value", value);
        encoding().set(prop, data);
        return this;
    }

    /** Sets a mark property by datum. */
    public View setPropertyDatum(String prop, JsonNode datum) {
        ObjectNode data = mapper.createObjectNode();
        data.set("datum", datum);
        encoding().set(prop, data);
        return this;
    }

    /**
     * Returns the view background's fill and stroke object.
     */
    public Background background() {
        return new Background(spec.has("view") ? (ObjectNode) spec.get("view") : spec.putObject("view"));
    }

    /**
     * Returns the defining properties of geographic projection, which will be
     * applied to shape path for "geoshape" marks and to latitude and
     * "longitude" channels for other marks.
     * @param type The cartographic projection to use.
     * @link https://vega.github.io/vega-lite/docs/projection.html#projection-types
     */
    public Projection projection(String type) {
        ObjectNode node = spec.putObject("projection");
        node.put("type", type);
        return new Projection(node);
    }

    @Override
    public View usermeta(JsonNode metadata) {
        super.usermeta(metadata);
        return this;
    }

    @Override
    public View usermeta(Object metadata) {
        super.usermeta(metadata);
        return this;
    }

    @Override
    public View background(String color) {
        super.background(color);
        return this;
    }

    @Override
    public View padding(int size) {
        super.padding(size);
        return this;
    }

    @Override
    public View padding(int left, int top, int right, int bottom) {
        super.padding(left, top, right, bottom);
        return this;
    }

    @Override
    public View autosize() {
        super.autosize();
        return this;
    }

    @Override
    public View autosize(String type, boolean resize, String contains) {
        super.autosize(type, resize, contains);
        return this;
    }

    @Override
    public View name(String name) {
        super.name(name);
        return this;
    }

    @Override
    public View description(String description) {
        super.description(description);
        return this;
    }

    @Override
    public View title(String title) {
        super.title(title);
        return this;
    }

    @Override
    public <T> View data(T[] data) {
        super.data(data);
        return this;
    }

    @Override
    public <T> View data(List<T> data) {
        super.data(data);
        return this;
    }

    @Override
    public View data(String url) {
        super.data(url);
        return this;
    }

    @Override
    public View json(String url, String property) {
        super.json(url, property);
        return this;
    }

    @Override
    public View topojson(String url, String conversion, String name) {
        super.topojson(url, conversion, name);
        return this;
    }

    @Override
    public View csv(String url) {
        super.csv(url);
        return this;
    }

    @Override
    public View csv(String url, Map<String, String> dataTypes) {
        super.csv(url, dataTypes);
        return this;
    }

    @Override
    public View tsv(String url) {
        super.tsv(url);
        return this;
    }

    @Override
    public View tsv(String url, Map<String, String> dataTypes) {
        super.tsv(url, dataTypes);
        return this;
    }

    @Override
    public View dsv(String url, String delimiter) {
        super.dsv(url, delimiter);
        return this;
    }

    @Override
    public View dsv(String url, String delimiter, Map<String, String> dataTypes) {
        super.dsv(url, delimiter, dataTypes);
        return this;
    }

    @Override
    public View transform(Transform... transforms) {
        super.transform(transforms);
        return this;
    }
}
