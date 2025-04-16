package atl.Contracts;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import static java.nio.charset.StandardCharsets.UTF_8;
	
@Contract(name = "Default",
	info = @Info(title = "default",
				description = "Here is a default example of the contract metadata structure",
				version = "1.0",
				license = 
						@License(name = "default",
								url = "www.google.com"),
								contact = @Contact(email = "default@gmail.com",
											name = "default",
											url = "www.google.com")))

@Default
public class contractEhr implements ContractInterface {

	public contractEhr() {
	}
	
	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void CreatePatient(Context ctx, String ID, String Name, String TypeId, int Phone, String Address, String Email, String ClinicalData){
		
		boolean exists = Patient(ctx,ID);
		if (exists){
			throw new RuntimeException("The asset "+ID+" already exists");
		}
		Patient asset = new Patient();
		asset.setID(ID);
		asset.setName(Name);
		asset.setTypeId(TypeId);
		asset.setPhone(Phone);
		asset.setAddress(Address);
		asset.setEmail(Email);
		asset.setClinicalData(ClinicalData);
		
		ctx.getStub().putState(ID, asset.toJSONString().getBytes(UTF_8));
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean PatientExists(Context ctx, String ID){
		
		byte [] buffer = ctx.GetStub().GetState(ID);	
		return (buffer != null && buffer.length > 0);
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void UpdatePatient(Context ctx, String ID, String ClinicalData){
		
		boolean exists = Patient(ctx,ID);
		if (!exists){
			throw new RuntimeException("The asset "+ID+" does not exist");
		}
		byte [] patientBytes = ctx.getStub().getState(ID);
		if (patientBytes == null || patientBytes.length == 0){
			throw new RuntimeException("The asset "+ID+" does not exist");
		}
		
		Patient patient = patient.fromJSONString(new String(patientBytes, UTF_8));    	
		patient.setClinicalData(ClinicalData);
		
		ctx.getStub().putState(ID, patient.toJSONString().getBytes(UTF_8));
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void DeletePatient(Context ctx, String ID){
		
		boolean exists = Patient(ctx,ID);
		if (!exists){
			throw new RuntimeException("The asset "+ID+" does not exist");
		}
		Patient asset = new Patient();
		asset.setID(ID);
		
		ctx.getStub().delState(ID, asset.toJSONString().getBytes(UTF_8));
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Patient ReadPatient(Context ctx, String ID){
		
		boolean exists = Patient(ctx,ID);
		if (!exists){
			throw new RuntimeException("The asset "+ID+" does not exist");
		}
		Patient asset = Patient.fromJSONString(new String(ctx.getStub().getState(ID),UTF_8));
		return asset;
	}
}
