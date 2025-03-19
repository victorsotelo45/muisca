package main

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

type EHRContract struct {
	contractapi.Contract
}
type Patient struct {
	ID string `json:"ID"`
	Name string `json:"name"`
	TypeId string `json:"typeId"`
	Phone string `json:"phone"`
	Age int `json:"age"`
	Address string `json:"address"`
	Email string `json:"email"`
	ClinicalData string `json:"clinicalData"`
}

func (ec *EHRContract) CreatePatient(ctx contractapi.TransactionContextInterface, ID string, Name string, TypeId string, Phone string, Age int, Address string, Email string, ClinicalData string) (error){

	exists, err := ec.PatientExists(ctx, ID)
	if err != nil {
		return err
	}
	if exists {
		return fmt.Errorf("The Patient %s already exists", ID)
	}

	Patient := &Patient{
		
		ID : ID,
		Name : Name,
		TypeId : TypeId,
		Phone : Phone,
		Age : Age,
		Address : Address,
		Email : Email,
		ClinicalData : ClinicalData,

	}

	PatientJSON, err := json.Marshal(Patient)
	if err != nil {
		return err
	}

	return ctx.GetStub().PutState(ID, PatientJSON)

}

func (ec *EHRContract) PatientExists(ctx contractapi.TransactionContextInterface, ID string) (bool, error){

	patientExistsJSON, err := ctx.GetStub().GetState(ID)
	if err != nil {
		return false, err
	}
	return patientExistsJSON != nil, nil

}

func (ec *EHRContract) UpdatePatient(ctx contractapi.TransactionContextInterface, ID string, ClinicalData string) (error){

	exists, err := ec.PatientExists(ctx, ID)
	if err != nil {
		return err
	}
	if !exists {
		return fmt.Errorf("Patient with ID %s does not exist", ID)
	}	
	
	patientJSON, err := ctx.GetStub().GetState(ID)
	if err != nil {
		return fmt.Errorf("Failed to read patient data: %v", err)
	}	
	
	var patient Patient
	err = json.Unmarshal(patientJSON, &patient)
	if err != nil {
		return fmt.Errorf("Failed to marshal updated patient data: %v", err)
	}	
	
	patient.ClinicalData = ClinicalData
	
	updatedPatientJSON, err := json.Marshal(patient)
	if err != nil {
		return fmt.Errorf("Failed to marshal updated patient data: %v", err)
	}
	
	return ctx.GetStub().PutState(ID, updatedPatientJSON)

}

func (ec *EHRContract) DeletePatient(ctx contractapi.TransactionContextInterface, ID string) (error){

	exists, err := ec.PatientExists(ctx, ID)
	if err != nil {
		return err
	}
	if !exists {
		return fmt.Errorf("Patient with ID %s does not exist", ID)
	}

	return ctx.GetStub().DelState(ID)

}

func (ec *EHRContract) ReadPatient(ctx contractapi.TransactionContextInterface, ID string) (*Patient, error){

patientJSON, err := ctx.GetStub().GetState(ID)
	if err != nil {
		return nil, err
	}
	if patientJSON == nil {
		return nil, fmt.Errorf("patient/] with ID %s does not exist", ID)
	}

	patient := new(Patient)
	err = json.Unmarshal(patientJSON, patient)
	if err != nil {
		return nil, err
	}

	return patient, nil
}



