//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.0-b170531.0717 
//         See <a href="https://jaxb.java.net/">https://jaxb.java.net/</a> 
//         Any modifications to this file will be lost upon recompilation of the source schema. 
//         Generated on: 2018.10.08 at 07:03:29 PM CDT 
//


package ca.wise.fgm.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CFB complex type.
 * 
 * <p>The following schema fragment specifies the expected         content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CFB"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice&gt;
 *         &lt;element name="C1"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;choice&gt;
 *                   &lt;element name="Default"&gt;
 *                     &lt;simpleType&gt;
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *                         &lt;enumeration value="C1"/&gt;
 *                         &lt;enumeration value="C2"/&gt;
 *                         &lt;enumeration value="C3"/&gt;
 *                         &lt;enumeration value="C4"/&gt;
 *                         &lt;enumeration value="C5"/&gt;
 *                         &lt;enumeration value="C6"/&gt;
 *                         &lt;enumeration value="C7"/&gt;
 *                         &lt;enumeration value="D1"/&gt;
 *                         &lt;enumeration value="M1"/&gt;
 *                         &lt;enumeration value="M2"/&gt;
 *                         &lt;enumeration value="M3"/&gt;
 *                         &lt;enumeration value="M4"/&gt;
 *                         &lt;enumeration value="O1a"/&gt;
 *                         &lt;enumeration value="O1b"/&gt;
 *                         &lt;enumeration value="O1ab"/&gt;
 *                         &lt;enumeration value="S1"/&gt;
 *                         &lt;enumeration value="S2"/&gt;
 *                         &lt;enumeration value="S3"/&gt;
 *                         &lt;enumeration value="NonFuel"/&gt;
 *                       &lt;/restriction&gt;
 *                     &lt;/simpleType&gt;
 *                   &lt;/element&gt;
 *                   &lt;element name="Custom"&gt;
 *                     &lt;complexType&gt;
 *                       &lt;complexContent&gt;
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                           &lt;attribute name="Eq56_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq56_P2" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq56_P3" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq56_P4" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq56_P5" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq57_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="Eq58_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
 *                           &lt;attribute name="CFB_Possible" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *                         &lt;/restriction&gt;
 *                       &lt;/complexContent&gt;
 *                     &lt;/complexType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/choice&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *         &lt;element name="D2"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;choice&gt;
 *                   &lt;element name="Default"&gt;
 *                     &lt;simpleType&gt;
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *                         &lt;enumeration value="D2"/&gt;
 *                       &lt;/restriction&gt;
 *                     &lt;/simpleType&gt;
 *                   &lt;/element&gt;
 *                 &lt;/choice&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CFB", namespace = "FBP_Schema", propOrder = {
    "c1",
    "d2"
})
public class CFB {

    @XmlElement(name = "C1", namespace = "FBP_Schema")
    protected CFB.C1 c1;
    @XmlElement(name = "D2", namespace = "FBP_Schema")
    protected CFB.D2 d2;

    /**
     * Gets the value of the c1 property.
     * 
     * @return
     *     possible object is
     *     {@link CFB.C1 }
     *     
     */
    public CFB.C1 getC1() {
        return c1;
    }

    /**
     * Sets the value of the c1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link CFB.C1 }
     *     
     */
    public void setC1(CFB.C1 value) {
        this.c1 = value;
    }

    /**
     * Gets the value of the d2 property.
     * 
     * @return
     *     possible object is
     *     {@link CFB.D2 }
     *     
     */
    public CFB.D2 getD2() {
        return d2;
    }

    /**
     * Sets the value of the d2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link CFB.D2 }
     *     
     */
    public void setD2(CFB.D2 value) {
        this.d2 = value;
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
     *         &lt;element name="Default"&gt;
     *           &lt;simpleType&gt;
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
     *               &lt;enumeration value="C1"/&gt;
     *               &lt;enumeration value="C2"/&gt;
     *               &lt;enumeration value="C3"/&gt;
     *               &lt;enumeration value="C4"/&gt;
     *               &lt;enumeration value="C5"/&gt;
     *               &lt;enumeration value="C6"/&gt;
     *               &lt;enumeration value="C7"/&gt;
     *               &lt;enumeration value="D1"/&gt;
     *               &lt;enumeration value="M1"/&gt;
     *               &lt;enumeration value="M2"/&gt;
     *               &lt;enumeration value="M3"/&gt;
     *               &lt;enumeration value="M4"/&gt;
     *               &lt;enumeration value="O1a"/&gt;
     *               &lt;enumeration value="O1b"/&gt;
     *               &lt;enumeration value="O1ab"/&gt;
     *               &lt;enumeration value="S1"/&gt;
     *               &lt;enumeration value="S2"/&gt;
     *               &lt;enumeration value="S3"/&gt;
     *               &lt;enumeration value="NonFuel"/&gt;
     *             &lt;/restriction&gt;
     *           &lt;/simpleType&gt;
     *         &lt;/element&gt;
     *         &lt;element name="Custom"&gt;
     *           &lt;complexType&gt;
     *             &lt;complexContent&gt;
     *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *                 &lt;attribute name="Eq56_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq56_P2" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq56_P3" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq56_P4" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq56_P5" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq57_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="Eq58_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
     *                 &lt;attribute name="CFB_Possible" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
     *               &lt;/restriction&gt;
     *             &lt;/complexContent&gt;
     *           &lt;/complexType&gt;
     *         &lt;/element&gt;
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
        "_default",
        "custom"
    })
    public static class C1 {

        @XmlElement(name = "Default", namespace = "FBP_Schema")
        protected String _default;
        @XmlElement(name = "Custom", namespace = "FBP_Schema")
        protected CFB.C1 .Custom custom;

        /**
         * Gets the value of the default property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDefault() {
            return _default;
        }

        /**
         * Sets the value of the default property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDefault(String value) {
            this._default = value;
        }

        /**
         * Gets the value of the custom property.
         * 
         * @return
         *     possible object is
         *     {@link CFB.C1 .Custom }
         *     
         */
        public CFB.C1 .Custom getCustom() {
            return custom;
        }

        /**
         * Sets the value of the custom property.
         * 
         * @param value
         *     allowed object is
         *     {@link CFB.C1 .Custom }
         *     
         */
        public void setCustom(CFB.C1 .Custom value) {
            this.custom = value;
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
         *       &lt;attribute name="Eq56_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq56_P2" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq56_P3" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq56_P4" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq56_P5" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq57_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="Eq58_P1" use="required" type="{http://www.heartlandsoftware.ca/xml/types}Double" /&gt;
         *       &lt;attribute name="CFB_Possible" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
         *     &lt;/restriction&gt;
         *   &lt;/complexContent&gt;
         * &lt;/complexType&gt;
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "")
        public static class Custom {

            @XmlAttribute(name = "Eq56_P1", required = true)
            protected String eq56P1;
            @XmlAttribute(name = "Eq56_P2", required = true)
            protected String eq56P2;
            @XmlAttribute(name = "Eq56_P3", required = true)
            protected String eq56P3;
            @XmlAttribute(name = "Eq56_P4", required = true)
            protected String eq56P4;
            @XmlAttribute(name = "Eq56_P5", required = true)
            protected String eq56P5;
            @XmlAttribute(name = "Eq57_P1", required = true)
            protected String eq57P1;
            @XmlAttribute(name = "Eq58_P1", required = true)
            protected String eq58P1;
            @XmlAttribute(name = "CFB_Possible", required = true)
            protected boolean cfbPossible;

            /**
             * Gets the value of the eq56P1 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq56P1() {
                return eq56P1;
            }

            /**
             * Sets the value of the eq56P1 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq56P1(String value) {
                this.eq56P1 = value;
            }

            /**
             * Gets the value of the eq56P2 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq56P2() {
                return eq56P2;
            }

            /**
             * Sets the value of the eq56P2 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq56P2(String value) {
                this.eq56P2 = value;
            }

            /**
             * Gets the value of the eq56P3 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq56P3() {
                return eq56P3;
            }

            /**
             * Sets the value of the eq56P3 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq56P3(String value) {
                this.eq56P3 = value;
            }

            /**
             * Gets the value of the eq56P4 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq56P4() {
                return eq56P4;
            }

            /**
             * Sets the value of the eq56P4 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq56P4(String value) {
                this.eq56P4 = value;
            }

            /**
             * Gets the value of the eq56P5 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq56P5() {
                return eq56P5;
            }

            /**
             * Sets the value of the eq56P5 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq56P5(String value) {
                this.eq56P5 = value;
            }

            /**
             * Gets the value of the eq57P1 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq57P1() {
                return eq57P1;
            }

            /**
             * Sets the value of the eq57P1 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq57P1(String value) {
                this.eq57P1 = value;
            }

            /**
             * Gets the value of the eq58P1 property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getEq58P1() {
                return eq58P1;
            }

            /**
             * Sets the value of the eq58P1 property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setEq58P1(String value) {
                this.eq58P1 = value;
            }

            /**
             * Gets the value of the cfbPossible property.
             * 
             */
            public boolean isCFBPossible() {
                return cfbPossible;
            }

            /**
             * Sets the value of the cfbPossible property.
             * 
             */
            public void setCFBPossible(boolean value) {
                this.cfbPossible = value;
            }

        }

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
     *         &lt;element name="Default"&gt;
     *           &lt;simpleType&gt;
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
     *               &lt;enumeration value="D2"/&gt;
     *             &lt;/restriction&gt;
     *           &lt;/simpleType&gt;
     *         &lt;/element&gt;
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
        "_default"
    })
    public static class D2 {

        @XmlElement(name = "Default", namespace = "FBP_Schema")
        protected String _default;

        /**
         * Gets the value of the default property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDefault() {
            return _default;
        }

        /**
         * Sets the value of the default property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDefault(String value) {
            this._default = value;
        }

    }

}
