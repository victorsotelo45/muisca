//Compilador
//SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

//import other contracts, interfaces or libraries

contract PatientManagement{
//Declaration of Variables
	string public ID; 
	string public Name;
    string public TypeId; 
	string public Phone; 
	int public Age; 
    string public Address; 
	string public Email;
    string public ClinicalData; 

//events
	event EjectFunctionAge(int agePatient);

//modifiers
	mapping(string => string) public MPatient;	

	struct Patient {
		string  ID; 
	    string  Name;
        string  TypeId; 
	    string  Phone; 
	    int Age; 
        string  Address; 
	    string  Email;
        string  ClinicalData; 
	}

	Patient [] public dataPatient;

//Functions

	function ReadPatient() public view returns(string memory){ 
		// Specific instructions for the function;
		return ID;
	}

	function CreatePatient(string memory _ID, string memory _Name, string memory _TypeId, string memory _Phone, int _Age, string memory _Address, string memory _Email, string memory _ClinicalData) public{ 
		// Specific instructions for the function;
		ID = _ID;
        Name = _Name;
        TypeId = _TypeId;
        Phone = _Phone;
        Age = _Age;
        Address = _Address;
        Email = _Email;
        ClinicalData = _ClinicalData;
	}

	function getIDPatient() public view returns (string memory ) {
		return ID; 
	}

	function setAge(int _Age) public{
		Age = _Age;
		//General instructions for the function;		
		//an event call is issued 
		emit EjectFunctionAge(Age);
	}

   	function getAge() public view returns (int) {
		return Age; 
	}
}
