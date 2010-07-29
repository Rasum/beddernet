/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package itu.beddernet.common;

/**
 * Interface that lets the implementing class be an observer. 
 * Another class can then call the update method sending the observed object 
 * whenever a relevant change has happened
 * @author Gober
 */
public interface Observer {
     public void update(Object arg);
}
