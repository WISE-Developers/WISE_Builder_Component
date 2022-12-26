//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 
//         See <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
//         Any modifications to this file will be lost upon recompilation of the source schema. 
//         Generated on: 2018.01.03 at 11:39:07 AM CST 
//


package ca.wise.fgm.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for WindSpeedGrid complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WindSpeedGrid"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="comments" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="startTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="endTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/&gt;
 *         &lt;element name="startTimeOfDay" type="{http://www.w3.org/2001/XMLSchema}duration"/&gt;
 *         &lt;element name="endTimeOfDay" type="{http://www.w3.org/2001/XMLSchema}duration"/&gt;
 *         &lt;element name="gridData"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;choice&gt;
 *                   &lt;element name="gridFiles"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;sequence&gt;
 *                             &lt;element name="gridFile" maxOccurs="8"&gt;
 *                               &lt;complexType&gt;
 *                                 &lt;complexContent&gt;
 *                                   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                                     &lt;all&gt;
 *                                       &lt;element name="fileName"&gt;
 *                                         &lt;complexType&gt;
 *                                           &lt;simpleContent&gt;
 *                                             &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *                                               &lt;attribute name="projectionFile" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *                                             &lt;/extension&gt;
 *                                           &lt;/simpleContent&gt;
 *                                         &lt;/complexType&gt;
 *                                       &lt;/element&gt;
 *                                     &lt;/all&gt;
 *                                     &lt;attribute name="speed" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                                     &lt;attribute name="sector"&gt;
 *                                       &lt;simpleType&gt;
 *                                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *                                           &lt;enumeration value="north"/&gt;
 *                                           &lt;enumeration value="northeast"/&gt;
 *                                           &lt;enumeration value="east"/&gt;
 *                                           &lt;enumeration value="southeast"/&gt;
 *                                           &lt;enumeration value="south"/&gt;
 *                                           &lt;enumeration value="southwest"/&gt;
 *                                           &lt;enumeration value="west"/&gt;
 *                                           &lt;enumeration value="northwest"/&gt;
 *                                           &lt;enumeration value="default"/&gt;
 *                                         &lt;/restriction&gt;
 *                                       &lt;/simpleType&gt;
 *                                     &lt;/attribute&gt;
 *                                   &lt;/restriction&gt;
 *                                 &lt;/complexContent&gt;
 *                               &lt;/complexType&gt;
 *                             &lt;/element&gt;
 *                           &lt;/sequence&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="gridDataRaw" type="{http://www.heartlandsoftware.ca/xml/types}WeatherGridData"/&gt;
 *                 &lt;/choice&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/all&gt;
 *       &lt;attribute name="type" use="required"&gt;
 *         &lt;simpleType&gt;
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *             &lt;enumeration value="direction"/&gt;
 *             &lt;enumeration value="speed"/&gt;
 *           &lt;/restriction&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WindSpeedGrid", propOrder = {

})
public class WindSpeedGrid {

    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types")
    protected String comments;
    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar startTime;
    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar endTime;
    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
    protected Duration startTimeOfDay;
    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
    protected Duration endTimeOfDay;
    @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
    protected WindSpeedGrid.GridData gridData;
    @XmlAttribute(name = "type", required = true)
    protected String type;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of the comments property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComments() {
        return comments;
    }

    /**
     * Sets the value of the comments property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComments(String value) {
        this.comments = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setStartTime(XMLGregorianCalendar value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setEndTime(XMLGregorianCalendar value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the startTimeOfDay property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getStartTimeOfDay() {
        return startTimeOfDay;
    }

    /**
     * Sets the value of the startTimeOfDay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setStartTimeOfDay(Duration value) {
        this.startTimeOfDay = value;
    }

    /**
     * Gets the value of the endTimeOfDay property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getEndTimeOfDay() {
        return endTimeOfDay;
    }

    /**
     * Sets the value of the endTimeOfDay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setEndTimeOfDay(Duration value) {
        this.endTimeOfDay = value;
    }

    /**
     * Gets the value of the gridData property.
     * 
     * @return
     *     possible object is
     *     {@link WindSpeedGrid.GridData }
     *     
     */
    public WindSpeedGrid.GridData getGridData() {
        return gridData;
    }

    /**
     * Sets the value of the gridData property.
     * 
     * @param value
     *     allowed object is
     *     {@link WindSpeedGrid.GridData }
     *     
     */
    public void setGridData(WindSpeedGrid.GridData value) {
        this.gridData = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected         content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;choice&gt;
     *         &lt;element name="gridFiles"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;sequence&gt;
     *                   &lt;element name="gridFile" maxOccurs="8"&gt;
     *                     &lt;complexType&gt;
     *                       &lt;complexContent&gt;
     *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                           &lt;all&gt;
     *                             &lt;element name="fileName"&gt;
     *                               &lt;complexType&gt;
     *                                 &lt;simpleContent&gt;
     *                                   &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
     *                                     &lt;attribute name="projectionFile" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
     *                                   &lt;/extension&gt;
     *                                 &lt;/simpleContent&gt;
     *                               &lt;/complexType&gt;
     *                             &lt;/element&gt;
     *                           &lt;/all&gt;
     *                           &lt;attribute name="speed" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                           &lt;attribute name="sector"&gt;
     *                             &lt;simpleType&gt;
     *                               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
     *                                 &lt;enumeration value="north"/&gt;
     *                                 &lt;enumeration value="northeast"/&gt;
     *                                 &lt;enumeration value="east"/&gt;
     *                                 &lt;enumeration value="southeast"/&gt;
     *                                 &lt;enumeration value="south"/&gt;
     *                                 &lt;enumeration value="southwest"/&gt;
     *                                 &lt;enumeration value="west"/&gt;
     *                                 &lt;enumeration value="northwest"/&gt;
     *                                 &lt;enumeration value="default"/&gt;
     *                               &lt;/restriction&gt;
     *                             &lt;/simpleType&gt;
     *                           &lt;/attribute&gt;
     *                         &lt;/restriction&gt;
     *                       &lt;/complexContent&gt;
     *                     &lt;/complexType&gt;
     *                   &lt;/element&gt;
     *                 &lt;/sequence&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="gridDataRaw" type="{http://www.heartlandsoftware.ca/xml/types}WeatherGridData"/&gt;
     *       &lt;/choice&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "gridFiles",
        "gridDataRaw"
    })
    public static class GridData {

        @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types")
        protected WindSpeedGrid.GridData.GridFiles gridFiles;
        @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types")
        protected WeatherGridData gridDataRaw;

        /**
         * Gets the value of the gridFiles property.
         * 
         * @return
         *     possible object is
         *     {@link WindSpeedGrid.GridData.GridFiles }
         *     
         */
        public WindSpeedGrid.GridData.GridFiles getGridFiles() {
            return gridFiles;
        }

        /**
         * Sets the value of the gridFiles property.
         * 
         * @param value
         *     allowed object is
         *     {@link WindSpeedGrid.GridData.GridFiles }
         *     
         */
        public void setGridFiles(WindSpeedGrid.GridData.GridFiles value) {
            this.gridFiles = value;
        }

        /**
         * Gets the value of the gridDataRaw property.
         * 
         * @return
         *     possible object is
         *     {@link WeatherGridData }
         *     
         */
        public WeatherGridData getGridDataRaw() {
            return gridDataRaw;
        }

        /**
         * Sets the value of the gridDataRaw property.
         * 
         * @param value
         *     allowed object is
         *     {@link WeatherGridData }
         *     
         */
        public void setGridDataRaw(WeatherGridData value) {
            this.gridDataRaw = value;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected         content contained within this class.
         * 
         * <pre>
         * &lt;complexType&gt;
         *   &lt;complexContent&gt;
         *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *       &lt;sequence&gt;
         *         &lt;element name="gridFile" maxOccurs="8"&gt;
         *           &lt;complexType&gt;
         *             &lt;complexContent&gt;
         *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
         *                 &lt;all&gt;
         *                   &lt;element name="fileName"&gt;
         *                     &lt;complexType&gt;
         *                       &lt;simpleContent&gt;
         *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
         *                           &lt;attribute name="projectionFile" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
         *                         &lt;/extension&gt;
         *                       &lt;/simpleContent&gt;
         *                     &lt;/complexType&gt;
         *                   &lt;/element&gt;
         *                 &lt;/all&gt;
         *                 &lt;attribute name="speed" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *                 &lt;attribute name="sector"&gt;
         *                   &lt;simpleType&gt;
         *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
         *                       &lt;enumeration value="north"/&gt;
         *                       &lt;enumeration value="northeast"/&gt;
         *                       &lt;enumeration value="east"/&gt;
         *                       &lt;enumeration value="southeast"/&gt;
         *                       &lt;enumeration value="south"/&gt;
         *                       &lt;enumeration value="southwest"/&gt;
         *                       &lt;enumeration value="west"/&gt;
         *                       &lt;enumeration value="northwest"/&gt;
         *                       &lt;enumeration value="default"/&gt;
         *                     &lt;/restriction&gt;
         *                   &lt;/simpleType&gt;
         *                 &lt;/attribute&gt;
         *               &lt;/restriction&gt;
         *             &lt;/complexContent&gt;
         *           &lt;/complexType&gt;
         *         &lt;/element&gt;
         *       &lt;/sequence&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "gridFile"
        })
        public static class GridFiles {

            @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
            protected List<WindSpeedGrid.GridData.GridFiles.GridFile> gridFile;

            /**
             * Gets the value of the gridFile property.
             * 
             * <p>
             * This accessor method returns a reference to the live list,
             * not a snapshot. Therefore any modification you make to the
             * returned list will be present inside the JAXB object.
             * This is why there is not a <CODE>set</CODE> method for the gridFile property.
             * 
             * <p>
             * For example, to add a new item, do as follows:
             * <pre>
             *    getGridFile().add(newItem);
             * </pre>
             * 
             * 
             * <p>
             * Objects of the following type(s) are allowed in the list
             * {@link WindSpeedGrid.GridData.GridFiles.GridFile }
             * 
             * 
             */
            public List<WindSpeedGrid.GridData.GridFiles.GridFile> getGridFile() {
                if (gridFile == null) {
                    gridFile = new ArrayList<WindSpeedGrid.GridData.GridFiles.GridFile>();
                }
                return this.gridFile;
            }


            /**
             * <p>Java class for anonymous complex type.
             * 
             * <p>The following schema fragment specifies the expected         content contained within this class.
             * 
             * <pre>
             * &lt;complexType&gt;
             *   &lt;complexContent&gt;
             *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
             *       &lt;all&gt;
             *         &lt;element name="fileName"&gt;
             *           &lt;complexType&gt;
             *             &lt;simpleContent&gt;
             *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
             *                 &lt;attribute name="projectionFile" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
             *               &lt;/extension&gt;
             *             &lt;/simpleContent&gt;
             *           &lt;/complexType&gt;
             *         &lt;/element&gt;
             *       &lt;/all&gt;
             *       &lt;attribute name="speed" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
             *       &lt;attribute name="sector"&gt;
             *         &lt;simpleType&gt;
             *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
             *             &lt;enumeration value="north"/&gt;
             *             &lt;enumeration value="northeast"/&gt;
             *             &lt;enumeration value="east"/&gt;
             *             &lt;enumeration value="southeast"/&gt;
             *             &lt;enumeration value="south"/&gt;
             *             &lt;enumeration value="southwest"/&gt;
             *             &lt;enumeration value="west"/&gt;
             *             &lt;enumeration value="northwest"/&gt;
             *             &lt;enumeration value="default"/&gt;
             *           &lt;/restriction&gt;
             *         &lt;/simpleType&gt;
             *       &lt;/attribute&gt;
             *     &lt;/restriction&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             * 
             * 
             */
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {

            })
            public static class GridFile {

                @XmlElement(namespace = "http://www.heartlandsoftware.ca/xml/types", required = true)
                protected WindSpeedGrid.GridData.GridFiles.GridFile.FileName fileName;
                @XmlAttribute(name = "speed")
                protected String speed;
                @XmlAttribute(name = "sector")
                protected String sector;

                /**
                 * Gets the value of the fileName property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link WindSpeedGrid.GridData.GridFiles.GridFile.FileName }
                 *     
                 */
                public WindSpeedGrid.GridData.GridFiles.GridFile.FileName getFileName() {
                    return fileName;
                }

                /**
                 * Sets the value of the fileName property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link WindSpeedGrid.GridData.GridFiles.GridFile.FileName }
                 *     
                 */
                public void setFileName(WindSpeedGrid.GridData.GridFiles.GridFile.FileName value) {
                    this.fileName = value;
                }

                /**
                 * Gets the value of the speed property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getSpeed() {
                    return speed;
                }

                /**
                 * Sets the value of the speed property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setSpeed(String value) {
                    this.speed = value;
                }

                /**
                 * Gets the value of the sector property.
                 * 
                 * @return
                 *     possible object is
                 *     {@link String }
                 *     
                 */
                public String getSector() {
                    return sector;
                }

                /**
                 * Sets the value of the sector property.
                 * 
                 * @param value
                 *     allowed object is
                 *     {@link String }
                 *     
                 */
                public void setSector(String value) {
                    this.sector = value;
                }


                /**
                 * <p>Java class for anonymous complex type.
                 * 
                 * <p>The following schema fragment specifies the expected         content contained within this class.
                 * 
                 * <pre>
                 * &lt;complexType&gt;
                 *   &lt;simpleContent&gt;
                 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
                 *       &lt;attribute name="projectionFile" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
                 *     &lt;/extension&gt;
                 *   &lt;/simpleContent&gt;
                 * &lt;/complexType&gt;
                 * </pre>
                 * 
                 * 
                 */
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {
                    "value"
                })
                public static class FileName {

                    @XmlValue
                    protected String value;
                    @XmlAttribute(name = "projectionFile")
                    protected String projectionFile;

                    /**
                     * Gets the value of the value property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getValue() {
                        return value;
                    }

                    /**
                     * Sets the value of the value property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setValue(String value) {
                        this.value = value;
                    }

                    /**
                     * Gets the value of the projectionFile property.
                     * 
                     * @return
                     *     possible object is
                     *     {@link String }
                     *     
                     */
                    public String getProjectionFile() {
                        return projectionFile;
                    }

                    /**
                     * Sets the value of the projectionFile property.
                     * 
                     * @param value
                     *     allowed object is
                     *     {@link String }
                     *     
                     */
                    public void setProjectionFile(String value) {
                        this.projectionFile = value;
                    }

                }

            }

        }

    }

}
