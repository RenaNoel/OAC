package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


import components.Bus;
import components.Memory;
import components.Register;
import components.Ula;

public class Architecture {
	
	private boolean simulation; //this boolean indicates if the execution is done in simulation mode.
								//simulation mode shows the components' status after each instruction
	
	
	private boolean halt;
	private Bus extbus1;
	private Bus intbus1;
	private Bus intbus2;
	private Memory memory;
	private int memorySize;
	private Register PC;
	private Register IR;
	private Register RPG;
	private Register RPG1;
	private Register RPG2;
	private Register RPG3;
	private Register Flags;
	private Ula ula;
	private Bus demux; //only for multiple register purposes
	
	private ArrayList<String> commandsList;
	private ArrayList<Register> registersList;
	
	

	/**
	 * Instanciates all components in this architecture
	 */
	private void componentsInstances() {
		//don't forget the instantiation order
		//buses -> registers -> ula -> memory
		extbus1 = new Bus();
		intbus1 = new Bus();
		intbus2 = new Bus();
		PC = new Register("PC", extbus1, intbus2);
		IR = new Register("IR", extbus1, intbus2);
		RPG = new Register("RPG0", extbus1, intbus1);
		RPG1 = new Register ("RPG1", extbus1, intbus1);
		RPG2 = new Register("RPG2", extbus1, intbus1);
		RPG3 = new Register("RPG3", extbus1, intbus1);
		Flags = new Register(3, intbus2);
		fillRegistersList();
		ula = new Ula(intbus1, intbus2);
		memorySize = 128;
		memory = new Memory(memorySize, extbus1);
		demux = new Bus(); //this bus is used only for multiple register operations
		
		fillCommandsList();
	}

	/**
	 * This method fills the registers list inserting into them all the registers we have.
	 * IMPORTANT!
	 * The first register to be inserted must be the default RPG
	 */
	private void fillRegistersList() {
		registersList = new ArrayList<Register>();
		registersList.add(RPG);
		registersList.add(RPG1);
		registersList.add(RPG2);
		registersList.add(RPG3);
		registersList.add(PC);
		registersList.add(IR);
		registersList.add(Flags);
	}

	/**
	 * Constructor that instanciates all components according the architecture diagram
	 */
	public Architecture() {
		componentsInstances();
		
		//by default, the execution method is never simulation mode
		simulation = false;
	}

	
	public Architecture(boolean sim) {
		componentsInstances();
		
		//in this constructor we can set the simoualtion mode on or off
		simulation = sim;
	}



	//getters
	
	protected Bus getExtbus1() {
		return extbus1;
	}

	protected Bus getIntbus1() {
		return intbus1;
	}

	protected Bus getIntbus2() {
		return intbus2;
	}

	protected Memory getMemory() {
		return memory;
	}

	protected Register getPC() {
		return PC;
	}

	protected Register getIR() {
		return IR;
	}

	protected Register getRPG() {
		return RPG;
	}
	
	protected Register getRPG1() {
		return RPG1;
	}
	
	protected Register getRPG2() {
		return RPG2;
	}

	protected Register getRPG3() {
		return RPG3;
	}
	
	protected Register getFlags() {
		return Flags;
	}

	protected Ula getUla() {
		return ula;
	}

	public ArrayList<String> getCommandsList() {
		return commandsList;
	}



	//all the microprograms must be impemented here
	//the instructions table is
	/*
	 *
			add addr (rpg <- rpg + addr)
			sub addr (rpg <- rpg - addr)
			jmp addr (pc <- addr)
			jz addr  (se bitZero pc <- addr)
			jn addr  (se bitneg pc <- addr)
			read addr (rpg <- addr)
			store addr  (addr <- rpg)
			ldi x    (rpg <- x. x must be an integer)
			inc    (rpg++)
			move regA regB (regA <- regB)
	 */
	
	/**
	 * This method fills the commands list arraylist with all commands used in this architecture
	 */
	protected void fillCommandsList() {
		commandsList = new ArrayList<String>();
		
		commandsList.add("addRegReg"); //0
		commandsList.add("addMemReg"); //1
		commandsList.add("addRegMem"); //2
		commandsList.add("addImmMem"); //3
		commandsList.add("subRegReg"); //4
		commandsList.add("subMemReg"); //5
		commandsList.add("subRegMem"); //6		
		commandsList.add("subImmMem"); //7
		commandsList.add("imulMemReg"); //8
		commandsList.add("imulRegMem"); //9
		commandsList.add("imulRegReg"); //10
		commandsList.add("moveMemReg"); //11
		commandsList.add("moveRegMem"); //12
		commandsList.add("moveRegReg"); //13
		commandsList.add("moveImmReg"); //14
		commandsList.add("incReg"); //15
		commandsList.add("incMem"); //16
		commandsList.add("jmp"); //17
		commandsList.add("jn"); //18
		commandsList.add("jz"); //19
		commandsList.add("jnz"); //20
		commandsList.add("jeq"); //21
		commandsList.add("jgt"); //22
		commandsList.add("jlw"); //23
		commandsList.add("ldi"); //24
	}

	
	/**
	 * This method is used after some ULA operations, setting the flags bits according the result.
	 * @param result is the result of the operation
	 * NOT TESTED!!!!!!!
	 */
	private void setStatusFlags(int result) {
		Flags.setBit(0, 0);
		Flags.setBit(1, 0);
		Flags.setBit(2, 0);
		if (result==0) { //bit 0 in flags must be 1 in this case
			Flags.setBit(0,1);
		}
		if (result<0) { //bit 1 in flags must be 1 in this case
			Flags.setBit(1,1);
		}
		if (result!=0) { //bit 2 in flags must be 1 in this case
			Flags.setBit(2, 1);
		}
	}

	public void ldi() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read(); 
		memory.read(); // the immediate is now in the external bus.
		demux.put(extbus1.get());
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
		PC.read();
		memory.read();
		registersStore();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	/**
	 * This method implements the microprogram for
	 * 					JMP address
	 * In the machine language this command number is 2, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where the PC is redirecto to)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register.
	 * So, the program is deviated
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. pc -> extbus //pc.read()
	 * 7. memory reads from extbus //this forces memory to write the data position in the extbus
	 * 8. pc <- extbus //pc.store() //pc was pointing to another part of the memory
	 * end
	 * @param address
	 */
	public void jmp() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read();
		memory.read();
		PC.store();
	}
	
	
	/**
	 * This method implements the microprogram for
	 * 					JZ address
	 * In the machine language this command number is 3, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the ZERO bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the ZERO bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. If FLAGS.bit0 is 1 (bit 0 is ZERO flag) then PC must be updated according the parameter
	 * 7. 	pc -> extbus //pc.read()
	 * 8. 	memory reads from extbus //this forces memory to write the data position in the extbus
	 * 9. 	pc <- extbus //pc.store() //pc was pointing to another part of the memory
	 * 10.ELSE //Flags.Bit0 is not 0. So, PC must be incremented to the next position
	 * 11.  ula incs //the position just after PC
	 * 12.  ula -> intbus2 //uma.read()
	 * 13.  pc <- intbus2 
	 * end
	 * @param address
	 */
	public void jz() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		if (Flags.getBit(0)==1) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	/**
	 * This method implements the microprogram for
	 * 					jn address
	 * In the machine language this command number is 4, and the address is in the position next to him
	 *    
	 * where address is a valid position in this memory architecture (where 
	 * the PC is redirected to, but only in the case the NEGATIVE bit in Flags is 1)
	 * The method reads the value from memory (position address) and 
	 * inserts it into the PC register if the NEG bit in Flags register is setted.
	 * So, the program is deviated conditionally
	 * The logic is
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the parameter
	 * 6. If FLAGS.bit1 is 1 (bit 1 is NEGATIVE flag) then PC must be updated according the parameter
	 * 7. 	pc -> extbus //pc.read()
	 * 8. 	memory reads from extbus //this forces memory to write the data position in the extbus
	 * 9. 	pc <- extbus //pc.store() //pc was pointing to another part of the memory
	 * 10.ELSE //Flags.Bit1 is not 0. So, PC must be incremented to the next position
	 * 11.  ula incs //the position just after PC
	 * 12.  ula -> intbus2 //ula.read()
	 * 13.  pc <- intbus2 
	 * end
	 * @param address
	 */
	public void jn() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		if (Flags.getBit(1)==1) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	public void jnz() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		if (Flags.getBit(2)==1) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	public void jeq() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read();
		memory.read();
		demux.put(extbus1.get());//points to the correct register
		registersRead(); //performs an internal store for the register identified into demux bus
		IR.store();
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
		PC.read();
		memory.read();
		demux.put(extbus1.get());
		registersInternalRead();
		IR.read();
		if (intbus1.get()==extbus1.get()) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	public void jgt() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read();
		memory.read();
		demux.put(extbus1.get());//points to the correct register
		registersRead(); //performs an internal store for the register identified into demux bus
		IR.store();
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
		PC.read();
		memory.read();
		demux.put(extbus1.get());
		registersInternalRead();
		IR.read();
		if (intbus1.get()>extbus1.get()) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	public void jlw() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the parameter address
		PC.read();
		memory.read();
		demux.put(extbus1.get());//points to the correct register
		registersRead(); //performs an internal store for the register identified into demux bus
		IR.store();
		ula.inc();
		ula.internalRead(1);
		PC.internalStore();
		PC.read();
		memory.read();
		demux.put(extbus1.get());
		registersInternalRead();
		IR.read();
		if (intbus1.get()<extbus1.get()) { 
			PC.read();
			memory.read();
			PC.store();
		}
		else {
			ula.inc();
			ula.internalRead(1);
			PC.internalStore();
		}
	}
	
	
	/**
	 * This method implements the microprogram for
	 * 					move <reg1> <reg2> 
	 * In the machine language this command number is 9
	 *    
	 * The method reads the two register ids (<reg1> and <reg2>) from the memory, in positions just after the command, and
	 * copies the value from the <reg1> register to the <reg2> register
	 * 
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the first parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the parameter (first regID) in the extbus
	 * 8. pc -> intbus2 //pc.read() //getting the second parameter
	 * 9. ula <-  intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store() now pc points to the second parameter
	 * 13. demux <- extbus //now the register to be operated is selected
	 * 14. registers -> intbus1 //this performs the internal reading of the selected register 
	 * 15. PC -> extbus (pc.read())the address where is the position to be read is now in the external bus 
	 * 16. memory reads from extbus //this forces memory to write the parameter (second regID) in the extbus
	 * 17. demux <- extbus //now the register to be operated is selected
	 * 18. registers <- intbus1 //thid rerforms the external reading of the register identified in the extbus
	 * 19. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 20. ula <- intbus2 //ula.store()
	 * 21. ula incs
	 * 22. ula -> intbus2 //ula.read()
	 * 23. pc <- intbus2 //pc.store()  
	 * 		  
	 */
	public void moveRegReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); // the first register id is now in the external bus.
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		PC.read();
		memory.read(); // the second register id is now in the external bus.
		demux.put(extbus1.get());//points to the correct register
		registersInternalStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	
	/**
	 * This method implements the microprogram for
	 * 					add <reg1> <reg2> 
	 * In the machine language this command number is 10
	 *    
	 * The method reads the two register ids (<reg1> and <reg2>) from the memory, in positions just after the command, and
	 * copies the SOMA from the <reg1> + <reg2>  to the <reg2> register
	 * 
	 * 1. pc -> intbus2 //pc.read()
	 * 2. ula <-  intbus2 //ula.store()
	 * 3. ula incs
	 * 4. ula -> intbus2 //ula.read()
	 * 5. pc <- intbus2 //pc.store() now pc points to the first parameter
	 * 6. pc -> extbus //(pc.read())the address where is the position to be read is now in the external bus 
	 * 7. memory reads from extbus //this forces memory to write the parameter (first regID) in the extbus
	 * 8. pc -> intbus2 //pc.read() //getting the second parameter
	 * 9. ula <-  intbus2 //ula.store()
	 * 10. ula incs
	 * 11. ula -> intbus2 //ula.read()
	 * 12. pc <- intbus2 //pc.store() now pc points to the second parameter
	 * 13. demux <- extbus //now the register to be operated is selected
	 * 14. registers -> intbus1 //this performs the internal reading of the selected register 
	 * 15. ula(0) <- intbus1
	 * 16. PC -> extbus (pc.read())the address where is the position to be read is now in the external bus 
	 * 17. memory reads from extbus //this forces memory to write the parameter (second regID) in the extbus
	 * 18. demux <- extbus //now the register to be operated is selected
	 * 19. registers -> intbus1
	 * 20. ula(1) <- intbus1
	 * 21. ula add
	 * 22. ula -> intbus1
	 * 23. registers <- intbus1 //thid rerforms the external reading of the register identified in the extbus
	 * 24. 10. pc -> intbus2 //pc.read() now pc must point the next instruction address
	 * 25. ula <- intbus2 //ula.store()
	 * 26. ula incs
	 * 27. ula -> intbus2 //ula.read()
	 * 28. pc <- intbus2 //pc.store()  
	 * 		  
	 */
	
	public void addRegReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); // the first register id is now in the external bus.
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.store(0);
		PC.read();
		memory.read(); // the second register id is now in the external bus.
		demux.put(extbus1.get());//points to the correct register
		registersInternalRead();
		ula.store(1);
		ula.add();
		ula.read(1);
		registersInternalStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void addMemReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); 
		memory.read();
		IR.store();
		IR.internalRead();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		PC.read();
		memory.read();
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.internalStore(0);
		ula.store(1);
		ula.add();
		ula.read(1);
		registersInternalStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void addRegMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); // the first register id is now in the external bus.
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.store(0);
		PC.read();
		memory.read();
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.add();
		ula.internalRead(1);
		IR.internalStore();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}	

	public void addImmMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter 
		PC.read(); 
		memory.read(); 
		IR.store();
		//PC.internalRead();
		//ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter 
		PC.read();
		memory.read();
		memory.read();
		IR.internalRead();
		ula.internalStore(0);
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.add();
		ula.internalRead(1);
		IR.internalStore();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void subRegReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); // the first register id is now in the external bus.
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.store(0);
		PC.read();
		memory.read(); // the second register id is now in the external bus.
		demux.put(extbus1.get());//points to the correct register
		registersInternalRead();
		ula.store(1);
		ula.sub();
		ula.read(1);
		registersInternalStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void subMemReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); 
		memory.read();
		IR.store();
		IR.internalRead();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		PC.read();
		memory.read();
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.internalStore(0);
		ula.store(1);
		ula.sub();
		ula.read(1);
		registersInternalStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void subRegMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter (the first reg id)
		PC.read(); 
		memory.read(); // the first register id is now in the external bus.
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the second reg id)
		demux.put(extbus1.get()); //points to the correct register
		registersInternalRead(); //starts the read from the register identified into demux bus
		ula.store(0);
		PC.read();
		memory.read();
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.sub();
		ula.internalRead(1);
		IR.internalStore();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}	
	
	public void subImmMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the first parameter 
		PC.read(); 
		memory.read(); 
		IR.store();
		//PC.internalRead();
		//ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter 
		PC.read();
		memory.read();
		memory.read();
		IR.internalRead();
		ula.internalStore(0);
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.sub();
		ula.internalRead(1);
		IR.internalStore();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void moveMemReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 
		PC.read(); 
		memory.read();
		memory.read();
		IR.store();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the reg id)
		PC.read();
		memory.read();
		demux.put(extbus1.get()); //points to the correct register
		IR.read();
		registersStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void moveRegMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 
		PC.read(); 
		memory.read();
		demux.put(extbus1.get()); //points to the correct register
		registersRead(); //performs a read for the register identified into demux bus
		IR.store();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the reg id)
		PC.read();
		memory.read();
		memory.read();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void moveImmReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 
		PC.read(); 
		memory.read();
		IR.store();
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the second parameter (the reg id)
		PC.read();
		memory.read();
		demux.put(extbus1.get()); //points to the correct register
		IR.read();
		registersStore(); //performs an internal store for the register identified into demux bus
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void incReg() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 
		PC.read(); 
		memory.read();
		demux.put(extbus1.get());
		registersInternalRead();
		ula.store(1);
		ula.inc();
		ula.read(1);
		setStatusFlags(intbus1.get());
		registersInternalStore();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	public void incMem() {
		PC.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); 
		PC.read(); 
		memory.read();
		memory.read();
		IR.store();
		IR.internalRead();
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		setStatusFlags(intbus1.get());
		IR.internalStore();
		memory.store();
		IR.read();
		memory.store();
		PC.internalRead(); //we need to make PC points to the next instruction address
		ula.internalStore(1);
		ula.inc();
		ula.internalRead(1);
		PC.internalStore(); //now PC points to the next instruction. We go back to the FETCH status.
	}
	
	
	public ArrayList<Register> getRegistersList() {
		return registersList;
	}

	/**
	 * This method performs an (external) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersRead() {
		registersList.get(demux.get()).read();
	}
	
	/**
	 * This method performs an (internal) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalRead() {
		registersList.get(demux.get()).internalRead();;
	}
	
	/**
	 * This method performs an (external) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersStore() {
		registersList.get(demux.get()).store();
	}
	
	/**
	 * This method performs an (internal) store toa register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalStore() {
		registersList.get(demux.get()).internalStore();;
	}



	/**
	 * This method reads an entire file in machine code and
	 * stores it into the memory
	 * NOT TESTED
	 * @param filename
	 * @throws IOException 
	 */
	public void readExec(String filename) throws IOException {
		   BufferedReader br = new BufferedReader(new		 
		   FileReader(filename+".dxf"));
		   String linha;
		   int i=0;
		   while ((linha = br.readLine()) != null) {
			     extbus1.put(i);
			     memory.store();
			   	 extbus1.put(Integer.parseInt(linha));
			     memory.store();
			     i++;
			}
			br.close();
	}
	
	/**
	 * This method executes a program that is stored in the memory
	 */
	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			fetch();
			decodeExecute();
		}

	}
	

	/**
	 * This method implements The decode proccess,
	 * that is to find the correct operation do be executed
	 * according the command.
	 * And the execute proccess, that is the execution itself of the command
	 */
	private void decodeExecute() {
		IR.internalRead(); //the instruction is in the internalbus2
		int command = intbus2.get();
		simulationDecodeExecuteBefore(command);
		switch (command) {
		case 0:
			addRegReg();
			break;
		case 1:
			addMemReg();
			break;
		case 2:
			addRegMem();
			break;
		case 3:
			addImmMem();
			break;
		case 4:
			subRegReg();
			break;
		case 5:
			subMemReg();
			break;
		case 6:
			subRegMem();
			break;
		case 7:
			subImmMem();
			break;
		/**case 8:
			imulMemReg();
			break;
		case 9:
			imulRegMem();
			break;
		case 10:
			imulRegReg();
			break;**/
		case 11:
			moveMemReg();
			break;
		case 12:
			moveRegMem();
			break;
		case 13:
			moveRegReg();
			break;
		case 14:
			moveImmReg();
			break;
		case 15:
			incReg();
			break;
		case 16:
			incMem();
			break;
		case 17:
			jmp();
			break;
		case 18:
			jn();
			break;
		case 19:
			jz();
			break;
		case 20:
			jnz();
			break;
		case 21:
			jeq();
			break;
		case 22:
			jgt();
			break;
		case 23:
			jlw();
			break;
		case 24:
			ldi();
			break;
		default:
			halt = true;
			break;
		}
		if (simulation)
			simulationDecodeExecuteAfter();
	}


	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED
	 * @param command 
	 */
	private void simulationDecodeExecuteBefore(int command) {
		System.out.println("----------BEFORE Decode and Execute phases--------------");
		String instruction;
		int parameter = 0;
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		if (command !=-1)
			instruction = commandsList.get(command);
		else
			instruction = "END";
		if (hasOperands(instruction)) {
			parameter = memory.getDataList()[PC.getData()+1];
			System.out.println("Instruction: "+instruction+" "+parameter);
		}
		else
			System.out.println("Instruction: "+instruction);
		if ("read".equals(instruction))
			System.out.println("memory["+parameter+"]="+memory.getDataList()[parameter]);
		
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED 
	 */
	private void simulationDecodeExecuteAfter() {
		String instruction;
		System.out.println("-----------AFTER Decode and Execute phases--------------");
		System.out.println("Internal Bus 1: "+intbus1.get());
		System.out.println("Internal Bus 2: "+intbus2.get());
		System.out.println("External Bus 1: "+extbus1.get());
		for (Register r:registersList) {
			System.out.println(r.getRegisterName()+": "+r.getData());
		}
		Scanner entrada = new Scanner(System.in);
		System.out.println("Press <Enter>");
		String mensagem = entrada.nextLine();
	}

	/**
	 * This method uses PC to find, in the memory,
	 * the command code that must be executed.
	 * This command must be stored in IR
	 * NOT TESTED!
	 */
	private void fetch() {
		PC.read();
		memory.read();
		IR.store();
		simulationFetch();
	}

	/**
	 * This method is used to show the components status in simulation conditions
	 * NOT TESTED!!!!!!!!!
	 */
	private void simulationFetch() {
		if (simulation) {
			System.out.println("-------Fetch Phase------");
			System.out.println("PC: "+PC.getData());
			System.out.println("IR: "+IR.getData());
		}
	}

	/**
	 * This method is used to show in a correct way the operands (if there is any) of instruction,
	 * when in simulation mode
	 * NOT TESTED!!!!!
	 * @param instruction 
	 * @return
	 */
	private boolean hasOperands(String instruction) {
		if ("inc".equals(instruction)) //inc is the only one instruction having no operands
			return false;
		else
			return true;
	}

	/**
	 * This method returns the amount of positions allowed in the memory
	 * of this architecture
	 * NOT TESTED!!!!!!!
	 * @return
	 */
	public int getMemorySize() {
		return memorySize;
	}
	
	public static void main(String[] args) throws IOException {
		Architecture arch = new Architecture(false);
		arch.readExec("program");
		arch.controlUnitEexec();
	}
	

}
