package com.test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.fazecast.jSerialComm.SerialPort;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public class TestROVController {
	
	final static double EPSILON = 1e-12;

	static double map(double valueCoord1, double startCoord1, double endCoord1, double startCoord2, double endCoord2) {

	    if (Math.abs(endCoord1 - startCoord1) < EPSILON) {
	        throw new ArithmeticException("/ 0");
	    }

	    double offset = startCoord2;
	    double ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
	    return ratio * (valueCoord1 - startCoord1) + offset;
	}
	
	static SerialPort chosenPort;
	
	static int servoVal1 = 0;
	static int servoVal2 = 0;
	static int servoVal3 = 0;	
	
	static int horSpeed = 100; 
	static int vertSpeed = 100; 
	
	static double potVal1 = 0.0;
	static double potVal2 = 0.0;
	static double potVal3 = 0.0;
	static double potVal4 = 0.0;
	
	static double forwardCommand = 0.0;
	static double turnCommand = 0.0;
	static double vertCommand = 0.0;
	static double pitchCommand = 0.0;
	
	static int mtr1Val=0;
	static int mtr2Val=0;
	static int mtr3Val=0;
	static int mtr4Val=0;

	public static void main(String[] args) {
		
		
		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("LHS-HORIZON Control Panel");
		window.setSize(400, 75);
		window.setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// create a drop-down box and connect button, then place them at the top of the window
		JComboBox<String> portList = new JComboBox<String>();
		JButton connectButton = new JButton("Connect");
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		window.add(topPanel, BorderLayout.NORTH);
		
		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for(int i = 0; i < portNames.length; i++)
			portList.addItem(portNames[i].getSystemPortName());
		
		// configure the connect button and use another thread to send data
		connectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				if(connectButton.getText().equals("Connect")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					if(chosenPort.openPort()) {
						connectButton.setText("Disconnect");
						portList.setEnabled(false);
						
						// create a new thread for sending data to the arduino
						Thread thread = new Thread(){

							@Override public void run() {
								
								ControllerManager controllers = new ControllerManager();
								controllers.initSDLGamepad();
								
								// wait after connecting, so the bootloader can finish
								try {Thread.sleep(100); } catch(Exception e) {}

								// enter an infinite loop that sends text to the arduino
								PrintWriter output = new PrintWriter(chosenPort.getOutputStream());
								while(true) {
									
									ControllerState currState = controllers.getState(0);
									
									if(!currState.isConnected) {
										break;
									}
									
									//Read Button Values to send for Servo operations
									if(currState.aJustPressed) {
										if (servoVal1 == 0) {
											servoVal1 = 1;
										} else {
											servoVal1 = 0;
										}
									}
									
									if(currState.lbJustPressed) {
										if (servoVal2 == 0) {
											servoVal2 = 1;
										} else {
											servoVal2 = 0;
										}
									}
									
									if(currState.rbJustPressed) {
										if (servoVal3 == 0) {
											servoVal3 = 1;
										} else {
											servoVal3 = 0;
										}
									}
									
									System.out.println ("ServoVal1: " + servoVal1);
									System.out.println ("ServoVal2: " + servoVal2);
									System.out.println ("ServoVal3: " + servoVal3);
									  
									potVal1 = (Math.round(currState.leftStickY * 100.0) / 100.0);
									potVal2 = (Math.round(currState.leftStickX * 100.0) / 100.0);
									potVal3 = (Math.round(currState.rightStickY * 100.0) / 100.0);
									potVal4 = (Math.round(currState.rightStickX * 100.0) / 100.0);
									  
									//System.out.println ("potVal1 value - " + potVal1);
									//System.out.println ("potVal2 value - " + potVal2);
									//System.out.println ("potVal3 value - " + potVal3);
									//System.out.println ("potVal4 value - " + potVal4);
									  		  
									forwardCommand = map(potVal1, -1.0, 1.0, -1 * horSpeed, horSpeed); 
									turnCommand = map(potVal2, -1.0, 1.0, -1 * horSpeed, horSpeed); 
									vertCommand = map(potVal3, -1.0, 1.0, -1 * vertSpeed, vertSpeed); 
									pitchCommand = map(potVal4, -1.0, 1.0, vertSpeed, -1 * vertSpeed); 
									  
//									System.out.println ("forwardCommand value - " + forwardCommand);
//									System.out.println ("turnCommand value - " + turnCommand);
//									System.out.println ("vertCommand value - " + vertCommand);
//									System.out.println ("pitchCommand value - " + pitchCommand);
									  
									mtr1Val = (int) Math.round (1500 + forwardCommand + turnCommand); 
							        mtr2Val = (int) Math.round (1500 + forwardCommand - turnCommand); 
									mtr3Val = (int) Math.round (1500 + vertCommand + pitchCommand); 
									mtr4Val = (int) Math.round (1500 + vertCommand - pitchCommand); 

									System.out.println ("Mtr1Val: " + mtr1Val);
									System.out.println ("Mtr2Val: " + mtr2Val);
									System.out.println ("Mtr3Val: " + mtr3Val);
									System.out.println ("Mtr4Val: " + mtr4Val);
									
									if ((mtr1Val < 1000) || (mtr1Val > 2000)) {
										mtr1Val = 1500;
									}
									if ((mtr2Val < 1000) || (mtr2Val > 2000)) {
										mtr2Val = 1500;
									}
									if ((mtr3Val < 1000) || (mtr3Val > 2000)) {
										mtr3Val = 1500;
									}
									if ((mtr4Val < 1000) || (mtr4Val > 2000)) {
										mtr4Val = 1500;
									}
									
									output.print("" + servoVal1 + servoVal2 + servoVal3 + mtr1Val + mtr2Val + mtr3Val + mtr4Val);
									output.flush();
									
									
									
									try {Thread.sleep(100); } catch(Exception e) {}
								}
								controllers.quitSDLGamepad();
							}
						};
						thread.start();
					}
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setText("Connect");
				}
			}
		});
		
		// show the window
		window.setVisible(true);
	}

}