package org.lejos.ev3.pcsample.charts;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;
import java.rmi.ConnectException;
import java.util.Properties;

import javax.swing.JFrame;

import lejos.remote.ev3.RMISampleProvider;
import lejos.remote.ev3.RemoteEV3;
import lejos.utility.Delay;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Sample program to draw a real-time bar chart of any EV3 sensor that supports the SampleProvider interface.
 * 
 * The program runs on the PC so it should be run as a Java program, not a leJOS EV3 program.
 * 
 * It uses the RMI remote API, and so does not require any program other than the menu to be running on the EV3.
 * 
 * The chart.properties file defines the sensor to use and all the other parameters for the sensor and the chart.
 * 
 * The default chart.properties file expects an EV3 IR sensor plugged into sensor port 1.
 * 
 * The default IP adddress in chart.properties is 10.0.1.1 which is usually correct if you are using a USB
 * connection. Otherwise, you should change chart.properties to specify the IP address of the EV3.
 * 
 * jfreechart is used to draw the bar chart.
 * 
 * @author Lawrie Griffiths
 *
 */
public class BarChart extends JFrame {
	private static final long serialVersionUID = 1L;
	private static String title = "EV3 sampling";
	private DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	private RemoteEV3 ev3;
	private RMISampleProvider sp;
	private String category;
	private String[] labels;
	private float frequency;
	private float factor;
	private boolean running = true;
	
	public BarChart(String host, String sensorClass, String portName, String mode, String category, String[] labels, 
			String units, float minValue, float maxValue, int windowWidth, int windowHeight, 
			float frequency, float factor) throws Exception {
		super(title);
		ev3 = new RemoteEV3(host);
		this.category = category;
		this.labels = labels;
		this.frequency = frequency;
		this.factor = factor;
		System.out.println("Creating remote sensor class: " + sensorClass + " on port " + portName + " with mode " + mode);
		sp = ev3.createSampleProvider(portName, sensorClass, (mode != null && mode.length() > 0 ? mode : null));
		JFreeChart chart = ChartFactory.createBarChart(title, category, units, dataset, 
				PlotOrientation.VERTICAL, true, true, false);
		
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(windowWidth, windowHeight));
		setContentPane(chartPanel);
		
		CategoryPlot plot = chart.getCategoryPlot();
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setRange(minValue, maxValue);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    addWindowListener(new WindowAdapter() {
	    	 
	        @Override
	        public void windowClosing(WindowEvent e) {
	        	try {
	        		running = false;
					if (sp != null) sp.close();
				} catch (Exception e1) {
					System.err.println("Exception closing sample provider");
				}
	        }
	    });
	}

	public void run() throws Exception {
        while(running) {
        	float[] sample = sp.fetchSample();
        	dataset.clear();
        	for(int i=0;i<labels.length;i++) {
        		dataset.addValue(sample[i] * factor, labels[i], category);
        	}
    		Delay.msDelay(((int) (1000f/frequency)));
        }
	}
	
	public static void main(String[] args) throws Exception {
		Properties p = new Properties();
		p.load(new FileReader("chart.properties"));
		
		String[] labels = p.getProperty("labels").split(",");
		BarChart demo;
		
        try {
        	demo = new BarChart(p.getProperty("host"), p.getProperty("class"), p.getProperty("port"), p.getProperty("mode"),
        		                     p.getProperty("category"), labels, p.getProperty("units"), 
        		                     Float.parseFloat(p.getProperty("min")), Float.parseFloat(p.getProperty("max")), 
        		                     Integer.parseInt(p.getProperty("width")), Integer.parseInt(p.getProperty("height")), 
        		                     Float.parseFloat(p.getProperty("frequency")),Float.parseFloat(p.getProperty("factor", "1.0")));
            demo.pack();
            demo.setVisible(true);
            demo.run();
        } catch (ConnectException e) {
        	System.err.println("Failed to connect - check the IP address in chart.properties.");
        }
	}
}
