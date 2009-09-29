
package org.apache.openejb.jee.was.v6.xmi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Replace complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Replace">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.omg.org/XMI}Difference">
 *       &lt;attribute name="position" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="replacement" type="{http://www.w3.org/2001/XMLSchema}IDREFS" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Replace")
public class Replace
    extends Difference
{

    @XmlAttribute
    protected String position;
    @XmlAttribute(name = "replacement")
    @XmlIDREF
    protected List<Object> replacements;

    /**
     * Gets the value of the position property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the value of the position property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPosition(String value) {
        this.position = value;
    }

    /**
     * Gets the value of the replacements property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the replacements property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReplacements().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getReplacements() {
        if (replacements == null) {
            replacements = new ArrayList<Object>();
        }
        return this.replacements;
    }

}