package org.team3128.drive;

import org.team3128.hardware.motor.MotorLink;
import org.team3128.listener.Listenable;
import org.team3128.listener.ListenerManager;

public class ArcadeDrive
{
    double _spdL, _spdR;

    MotorLink _driveLeft;

    MotorLink _driveRight;

    ListenerManager _listenerManager;
    
    public ArcadeDrive(MotorLink driveLeft, MotorLink driveRight, ListenerManager listenerManager)
    {
    	_driveLeft = driveLeft;
    	_driveRight = driveRight;
    	_listenerManager = listenerManager;
    }
    
	//threshold below which joystick movements are ignored.
	final static double thresh = 0.2;
    public void steer()
    {
    	//read joystick values
    	double x1 = _listenerManager.getRawDouble(Listenable.JOY1X);
    	x1 = Math.abs(x1) > thresh ? x1 : 0.0;
    	
    	
    	double y1 = _listenerManager.getRawDouble(Listenable.JOY1Y);
    	y1 = Math.abs(y1) > thresh ? y1 : 0.0;

    	_spdR = (y1 + x1) / 2;
    	_spdL = (y1 - x1) / 2;

    	_driveLeft.setSpeed(_spdL);
    	_driveRight.setSpeed(_spdR);
    }
}
