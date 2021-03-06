package org.team3128.autonomous.commands;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.CommandGroup;

/*        _
 *       / \ 
 *      / _ \
 *     / [ ] \
 *    /  [_]  \
 *   /    _    \
 *  /    (_)    \
 * /_____________\
 * -----------------------------------------------------
 * UNTESTED CODE!
 * This class has never been tried on an actual robot.
 * It may be non or partially functional.
 * Do not make any assumptions as to its behavior!
 * And don't blink.  Not even for a second.
 * -----------------------------------------------------*/

/**
 *	This command is constructed with a group of commands.
 * They will be run in parallel when the command is invoked.
 */
public class CmdRunInParallel extends CommandGroup {
    
    public CmdRunInParallel(Command... commands)
    {
        if(commands == null || commands.length < 1)
        {
        	throw new IllegalArgumentException("You must provide at least one command!");
        }
        addSequential(commands[0]);
        for(int index = 1; index < commands.length; ++index)
        {
        	addParallel(commands[index]);
        }
    }
}
