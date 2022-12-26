//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 
//         See <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
//         Any modifications to this file will be lost upon recompilation of the source schema. 
//         Generated on: 2018.03.20 at 10:19:33 PM CDT 
//


package ca.wise.fgm.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for BurnCondition complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BurnCondition"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="date" use="required" type="{http://www.w3.org/2001/XMLSchema}date" /&gt;
 *       &lt;attribute name="startTime" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="endTime" type="{http://www.w3.org/2001/XMLSchema}duration" /&gt;
 *       &lt;attribute name="fwiGreater" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *       &lt;attribute name="isiGreater" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *       &lt;attribute name="wsGreater" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *       &lt;attribute name="rhLess" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BurnCondition")
public class BurnCondition {

    @XmlAttribute(name = "date", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar date;
    @XmlAttribute(name = "startTime")
    protected Duration startTime;
    @XmlAttribute(name = "endTime")
    protected Duration endTime;
    @XmlAttribute(name = "fwiGreater")
    protected String fwiGreater;
    @XmlAttribute(name = "isiGreater")
    protected String isiGreater;
    @XmlAttribute(name = "wsGreater")
    protected String wsGreater;
    @XmlAttribute(name = "rhLess")
    protected String rhLess;

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setDate(XMLGregorianCalendar value) {
        this.date = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setStartTime(Duration value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setEndTime(Duration value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the fwiGreater property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFwiGreater() {
        return fwiGreater;
    }

    /**
     * Sets the value of the fwiGreater property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFwiGreater(String value) {
        this.fwiGreater = value;
    }

    /**
     * Gets the value of the isiGreater property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIsiGreater() {
        return isiGreater;
    }

    /**
     * Sets the value of the isiGreater property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIsiGreater(String value) {
        this.isiGreater = value;
    }

    /**
     * Gets the value of the wsGreater property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWsGreater() {
        return wsGreater;
    }

    /**
     * Sets the value of the wsGreater property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWsGreater(String value) {
        this.wsGreater = value;
    }

    /**
     * Gets the value of the rhLess property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRhLess() {
        return rhLess;
    }

    /**
     * Sets the value of the rhLess property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRhLess(String value) {
        this.rhLess = value;
    }

}
