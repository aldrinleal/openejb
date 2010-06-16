/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.07.13 at 11:14:43 PM EDT 
//


package org.apache.openejb.jee;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *                 
 *                 The "navigation-case" element describes a particular 
 *                 combination of conditions that must match for this case to 
 *                 be executed, and the view id of the component tree that 
 *                 should be selected next.
 *                 
 *             
 * 
 * <p>Java class for faces-config-navigation-caseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="faces-config-navigation-caseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://java.sun.com/xml/ns/javaee}descriptionGroup"/>
 *         &lt;element name="from-action" type="{http://java.sun.com/xml/ns/javaee}faces-config-from-actionType" minOccurs="0"/>
 *         &lt;element name="from-outcome" type="{http://java.sun.com/xml/ns/javaee}string" minOccurs="0"/>
 *         &lt;element name="if" type="{http://java.sun.com/xml/ns/javaee}faces-config-ifType" minOccurs="0"/>
 *         &lt;element name="to-view-id" type="{http://java.sun.com/xml/ns/javaee}faces-config-valueType"/>
 *         &lt;element name="redirect" type="{http://java.sun.com/xml/ns/javaee}faces-config-redirectType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 48 *
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "faces-config-navigation-caseType", propOrder = {
    "description",
    "displayName",
    "icon",
    "fromAction",
    "fromOutcome",
        "_if",
    "toViewId",
    "redirect"
})
public class FacesNavigationCase {

    protected List<DescriptionType> description;
    @XmlElement(name = "display-name")
    protected List<java.lang.String> displayName;
    protected List<Icon> icon;
    @XmlElement(name = "from-action")
    protected FacesFromAction fromAction;
    @XmlElement(name = "from-outcome")
    protected java.lang.String fromOutcome;
    //this is a faces EL expression
    @XmlElement(name = "if")
    protected String _if;
    @XmlElement(name = "to-view-id", required = true)
    protected java.lang.String toViewId;
    protected FacesRedirect redirect;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DescriptionType }
     * 
     * 
     */
    public List<DescriptionType> getDescription() {
        if (description == null) {
            description = new ArrayList<DescriptionType>();
        }
        return this.description;
    }

    /**
     * Gets the value of the displayName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the displayName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisplayName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link java.lang.String }
     * 
     * 
     */
    public List<java.lang.String> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<java.lang.String>();
        }
        return this.displayName;
    }

    /**
     * Gets the value of the icon property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the icon property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIcon().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Icon }
     * 
     * 
     */
    public List<Icon> getIcon() {
        if (icon == null) {
            icon = new ArrayList<Icon>();
        }
        return this.icon;
    }

    /**
     * Gets the value of the fromAction property.
     * 
     * @return
     *     possible object is
     *     {@link FacesFromAction }
     *     
     */
    public FacesFromAction getFromAction() {
        return fromAction;
    }

    /**
     * Sets the value of the fromAction property.
     * 
     * @param value
     *     allowed object is
     *     {@link FacesFromAction }
     *     
     */
    public void setFromAction(FacesFromAction value) {
        this.fromAction = value;
    }

    /**
     * Gets the value of the fromOutcome property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getFromOutcome() {
        return fromOutcome;
    }

    /**
     * Sets the value of the fromOutcome property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setFromOutcome(java.lang.String value) {
        this.fromOutcome = value;
    }

    /**
     * Gets the value of the if property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getIf() {
        return _if;
    }

    /**
     * Sets the value of the if property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setIf(String value) {
        this._if = value;
    }

    /**
     * Gets the value of the toViewId property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getToViewId() {
        return toViewId;
    }

    /**
     * Sets the value of the toViewId property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setToViewId(java.lang.String value) {
        this.toViewId = value;
    }

    /**
     * Gets the value of the redirect property.
     * 
     * @return
     *     possible object is
     *     {@link FacesRedirect }
     *     
     */
    public FacesRedirect getRedirect() {
        return redirect;
    }

    /**
     * Sets the value of the redirect property.
     * 
     * @param value
     *     allowed object is
     *     {@link FacesRedirect }
     *     
     */
    public void setRedirect(FacesRedirect value) {
        this.redirect = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

}
