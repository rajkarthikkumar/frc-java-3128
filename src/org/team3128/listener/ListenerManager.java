package org.team3128.listener;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.team3128.Log;
import org.team3128.Options;
import org.team3128.util.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import edu.wpi.first.wpilibj.Joystick;

/**
 *This class combines the functions of XControl and ListenerManager from the old robot code.
 *It is constructed with one Joystick object, which confusingly seems to be the wpilib metaphor for an entire controller.
 *It polls the controller at a set interval, and invokes listeners
 *whenever a value they're set for has changed (the button listeners are set for either call on up or down).
 *Listeners are run on the object's polling thread, and will need to be passed a reference
 *to the listener manager somehow if they need control data.
 *You may register the same instance of a listener for as many
 *controls as you like, but it will only be invoked once per polling cycle no matter how many of its registered
 *controls have changed.  However, if you register two different instances of the same listener object for two different controls, 
 *those listeners will both be invoked if both controls change. 
 * 
 * @author Jamie
 *
 */
public class ListenerManager
{
	
	//when this is locked no one should touch _joystickValues or _buttonValues
	ReentrantLock _controlValuesMutex;
	
	//maps the listeners to the control inputs
	Multimap<Listenable, IListenerCallback> _listeners;

	//wpilib object which represents a controller
	Joystick _joystick;

	EnumMap<Listenable, Double> _joystickValues;

	EnumMap<Listenable, Boolean> _buttonValues;

	Thread _thread;
	
	//construct from existing Joystick
	public ListenerManager(Joystick joystick)
	{
		_controlValuesMutex = new ReentrantLock();
		_listeners = Multimaps.synchronizedListMultimap(ArrayListMultimap.<Listenable, IListenerCallback>create());
        _joystick = joystick;
        
        Pair<EnumMap<Listenable, Boolean>, EnumMap<Listenable, Double>> controlValues = pollControls();
        
		_joystickValues = controlValues.right;
		_buttonValues = controlValues.left;
		_thread = new Thread(this::run, "ListenerManager Thread");
		_thread.start();
	}
	
	//add listener for given listenable
	//multiple listeners can be added for the same listenable
	public void addListener(Listenable key, IListenerCallback listener)
	{
		_listeners.put(key, listener);
	}

	//remove all listeners, period
	public void removeAllListeners()
	{
		_listeners.clear();
	}

	//remove all listeners set for the given listener
	//note that removeAllListenersForControl(Listenable.AUP) is NOT the same as removeAllListenersForControl(Listenable.ADOWN)
	public void removeAllListenersForControl(Listenable listener)
	{
		_listeners.removeAll(listener);
	}

	//returns the boolean value of a button listenable (between A and R3).
	//returns true if presses whether $buttonUP or $buttonDOWN was given
	//Does bounds checking, throws if value is out of range.
	public boolean getRawBool(Listenable listenable)
	{
		//check that this listenable is one of the boolean valued ones
		if(listenable.ordinal() >= Listenable.ADOWN.ordinal() && listenable.ordinal() <= Listenable.R3DOWN.ordinal())
		{
			_controlValuesMutex.lock();
			Boolean retval = false;
			
			retval = _buttonValues.get(listenable);
			_controlValuesMutex.unlock();

			if(retval == null)
			{
				Log.recoverable("ListenerManager", "Attempt to read data from ListenerManager whose thread has not finished starting");
				return false;
			}
			
			return retval;
		}
		//if that came up empty, try the up values of those same controls
		else if(listenable.ordinal() >= Listenable.AUP.ordinal() && listenable.ordinal() <= Listenable.R3UP.ordinal())
		{
			_controlValuesMutex.lock();
			Boolean retval = false;
			
			retval = _buttonValues.get(Listenable.values()[listenable.ordinal() - 20]);

			_controlValuesMutex.unlock();
			
			if(retval == null)
			{
				Log.recoverable("ListenerManager", "Attempt to read data from ListenerManager whose thread has not finished starting");
				return false;
			}
			
			return retval;
		}
		else
		{
			throw new RuntimeException("Attempt to get boolean value of control listenable " + listenable.ordinal() + " which is not a boolean");
		}
	}

	//returns the double value of an axis listenable (between JOY1X and TRIGGERS).
	//Does bounds checking, throws if value is out of range.
	public double getRawDouble(Listenable listenable)
	{
		//check that this listenable is one of the double valued ones
		if(listenable.ordinal() >= Listenable.JOY1X.ordinal() && listenable.ordinal() <= Listenable.JOY2Y.ordinal())
		{
			_controlValuesMutex.lock();
			Double retval = 0.0;
			
			retval = _joystickValues.get(listenable);
			
			if(retval == null)
			{
				Log.recoverable("ListenerManager", "Attempt to read data from ListenerManager whose thread has not finished starting");
				return 0.0;
			}
			_controlValuesMutex.unlock();
			return retval;
		}
		else
		{
			throw new RuntimeException("Attempt to get double value of control listenable " + listenable.ordinal() + " which is not a double");
		}
	}

	Pair<EnumMap<Listenable, Boolean>, EnumMap<Listenable, Double>> pollControls()
	{
		EnumMap<Listenable, Boolean> buttonValues = new EnumMap<Listenable, Boolean>(Listenable.class);
		EnumMap<Listenable, Double> joystickValues = new EnumMap<Listenable, Double>(Listenable.class);
	
		_controlValuesMutex.lock();

		//read button values
		for(int counter = Listenable.ADOWN.ordinal(); counter <= Listenable.R3DOWN.ordinal() ; counter++)
		{
			buttonValues.put(Listenable.values()[counter], _joystick.getRawButton(counter));
		}
		
		//read joystick values
		//NOTE: these may change when we move from the emulator to the actual robot.
		//if you're reading this after 2015 season, well, I guess we forgot to remove this comment
		for(int counter = Listenable.JOY1X.ordinal(); counter <= Listenable.JOY2Y.ordinal(); counter++)
		{
			joystickValues.put(Listenable.values()[counter], _joystick.getRawAxis(counter - 9));
		}
		
		_controlValuesMutex.unlock();
		
		return new Pair<EnumMap<Listenable, Boolean>, EnumMap<Listenable, Double>>(buttonValues, joystickValues);
	}

	void run()
	{
		while(true)
		{
			Pair<EnumMap<Listenable, Boolean>, EnumMap<Listenable, Double>> newValues = pollControls();

			//test if values have changed
			if((newValues.left != _buttonValues) || (newValues.right != _joystickValues))
			{
				Set<IListenerCallback> listenersToInvoke = new HashSet<IListenerCallback>();

				//loop through button values
				for(int counter = Listenable.ADOWN.ordinal(); counter <= Listenable.R3DOWN.ordinal() ; counter++)
				{
					Listenable currentListenable = Listenable.values()[counter];
					//has this button been pressed?
					if((!_buttonValues.get(currentListenable)) && (newValues.left.get(currentListenable)))
					{
						//get all its registered listeners
						Collection<IListenerCallback> foundListeners = _listeners.get(Listenable.values()[counter]);

						if(!foundListeners.isEmpty())
						{
							//loop through them
							for(IListenerCallback callback : foundListeners)
							{
								listenersToInvoke.add(callback);
							}
						}

					}
					//has this button just stopped being pressed?
					if((_buttonValues.get(currentListenable)) && (!newValues.left.get(currentListenable)))
					{
						//get all its registered listeners
						//increment counter by 20 to get button up listeners
						Collection<IListenerCallback> foundListeners = _listeners.get(Listenable.values()[counter + 15]);

						if(!foundListeners.isEmpty())
						{
							//loop through them
							for(IListenerCallback callback : foundListeners)
							{
								listenersToInvoke.add(callback);
							}
						}

					}
				}

				//loop through joystick values
				for(int counter = Listenable.JOY1X.ordinal(); counter <= Listenable.JOY2Y.ordinal() ; counter++)
				{
					Listenable currentListenable = Listenable.values()[counter];
					//has this particular value changed?
					if(_joystickValues.get(currentListenable) != newValues.right.get(currentListenable))
					{
						//get all its registered listeners
						Collection<IListenerCallback> foundListeners = _listeners.get(currentListenable);

						if(!foundListeners.isEmpty())
						{
							//loop through them
							for(IListenerCallback callback : foundListeners)
							{
								listenersToInvoke.add(callback);
							}
						}

					}
				}


				//update class variables to match new data
				{
					_controlValuesMutex.lock();
					_buttonValues = newValues.left;
					_joystickValues = newValues.right;
					_controlValuesMutex.unlock();
				}

				//invoke handlers
				for(IListenerCallback listener: listenersToInvoke)
				{
					try
					{
						listener.listenerCallback();
					}
					catch(RuntimeException error)
					{
						Log.recoverable("ListenerManager", "Caught a " + error.toString() + " from a control listener: " + error.getMessage());
						error.printStackTrace();
					}
				}
			}
			try
			{
				Thread.sleep(Options.instance()._listenerManagerUpdateFrequency);
			}
			catch(InterruptedException error)
			{
				return;
			}
		}
	}
	
	/**
	 * Grumble grumble, no destructors, grumble grumble
	 * 
	 * Shut down the listener thread.
	 */
	public void shutDown()
	{
		_thread.interrupt();
	}
	
	/**
	 * Start the thread if it's been shut down
	 */
	public void reStart()
	{
		if(!_thread.isAlive())
		{
			_thread = new Thread(this::run, "ListenerManager");
		}
	}
}
