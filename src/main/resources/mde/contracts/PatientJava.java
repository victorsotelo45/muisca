package org.example;

import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

@DataType()
public class Patient{
	@Property
	public String ID;
	@Property
	public String Name;
	@Property
	public String TypeId;
	@Property
	public int Phone;
	@Property
	public String Age;
	@Property
	public String Address;
	@Property
	public String Email;
	@Property
	public String ClinicalData;

	public Patient(){
	}
	public String getID(){
		return ID;
	}
	public String getName(){
		return Name;
	}
	public String getTypeId(){
		return TypeId;
	}
	public int getAge(){
		return Age;
	}
	public String getPhone(){
		return Phone;
	}
	public String getAddress(){
		return Address;
	}
	public String getEmail(){
		return Email;
	}
	public String getClinicalData(){
		return ClinicalData;
	}
	public void setID(String ID){
		this.ID = ID;
	}
	public void setName(String Name){
		this.Name = Name;
	}
	public void setTypeId(String TypeId){
		this.TypeId = TypeId;
	}
	public void setPhone(string Phone){
		this.Phone = Phone;
	}
	public void setAge(int Age){
		this.Age = Age;
	}
	public void setAddress(String Address){
		this.Address = Address;
	}
	public void setEmail(String Email){
		this.Email = Email;
	}
	public void setClinicalData(String ClinicalData){
		this.ClinicalData = ClinicalData;
	}
	public String toJSONString() {
		return new JSONObject(this).toString();
	}

	public static Patient fromJSONString(String json) {
		JSONObject jsonObject = new JSONObject(json);
		String ID = jsonObject.getString("ID");
		String Name = jsonObject.getString("Name");
		String TypeId = jsonObject.getString("TypeId");
		String Phone = jsonObject.getString("Phone");
		int Age = jsonObject.getInt("Age");
		String Address = jsonObject.getString("Address");
		String Email = jsonObject.getString("Email");
		String ClinicalData = jsonObject.getString("ClinicalData");
	    Patient asset = new Patient();
		asset.setID(ID);
		asset.setName(Name);
		asset.setTypeId(TypeId);
		asset.setPhone(Phone);
		asset.setAge(Age);
		asset.setAddress(Address);
		asset.setEmail(Email);
		asset.setClinicalData(ClinicalData);
	    return asset;
	}
}
