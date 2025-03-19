
#####   Mechanism for UnIversal Smart ContrAct - MUISCA 

This repository contains all the elements related to the MUISCA project. Each of them is described below:

Metamodels: Contains the metamodels generated in the project. There is one folder for each metamodel, each containing:

	* .aird: is a file that contains information about the layout of elements in diagrams within EMF, such as Ecore diagrams, class diagrams, package diagrams, etc. It is used by Eclipse to store layout and presentation information of EMF models.

	* .ecore: this file defines the structure and semantics of a model using the Ecore language. An .ecore file defines the elements, classes, attributes and relationships that can exist in an EMF model. It is the basis for defining other domain-specific models in EMF.

	* .genmodel: this file defines how code artifacts are generated from an Ecore metamodel. It contains information about the generation rules, such as the generation of Java classes, interfaces, methods, etc. It is used by EMF to automatically generate code from an Ecore metamodel.

 	* .jpg: is a image file with extension jpg, is an image of the main class diagram of the metamodel. 

 	The four previous files are in this folder for the following three metamodels:

	* EthereumSolidity: metamodel for the Ethereum platform, for the Solidity programming language.
	* HyperledgerJava: metamodel for the Hyperledger platform, for the Java programming language.
	* HyperledgerGo: metamodel for the Hyperledger platform, for the Go programming language.


Models: It contains the different models generated in each of the levels, in this case the following:

	* SContractPatient.xmi: model of the SC generated with IContractML
	* SContractPatient.xmi. Model generated in the IContractML to EthereumSolidity Transformation.

Contracts: contains the generated smart contracts, for each of the blockchain platforms and programming languages, in this case:

	* PatientManagement.sol: smart contract generated in EthereumSolidity.
	* PatientJava.java and ContractPatient.java: Contain the patient class and the contract to manage a patient, generated with the HyperledgerJava metamodel.
	* PatientGo: Smart contract generated in HyperledgerGo.

Transformations: In this directory we find the m2m and m2t transformations, created in ATL and acceleo, respectively.In this directory there are two subfolders:

	* ATL: contains the m2m transformations, for example, Go2Java, is the transformation to transform a smart contract from Go to Java.
	* Acceleo: contains the m2t transformations, which are used to generate the source code of the smart contracts. Here we find 3 files with mtl extension, these are:
		- generateHyperledgerJava.mtl, generates the source code of a smart contract for the Hyperledger platform for java language.
		- generateHyperledgerGo.mtl, generates the source code of a smart contract for the Hyperledger platform for the Go language.
		- generateEthereumSolidity.mtl, generates the source code of a smart contract for the Ethereum platform for the solidity language.


This repository contains all the elements related to the MUISCA project. Each of them is described below:

     1. Metamodels: Contains the metamodels generated in the project. There is one folder for each metamodel, each containing:

.aird: is a file that contains information about the layout of elements in diagrams within EMF, such as Ecore diagrams, class diagrams, package diagrams, etc. It is used by Eclipse to store layout and presentation information of EMF models.
.ecore: this file defines the structure and semantics of a model using the Ecore language. An .ecore file defines the elements, classes, attributes and relationships that can exist in an EMF model. It is the basis for defining other domain-specific models in EMF.
.genmodel: this file defines how code artifacts are generated from an Ecore metamodel. It contains information about the generation rules, such as the generation of Java classes, interfaces, methods, etc. It is used by EMF to automatically generate code from an Ecore metamodel.
.jpg: is a image file with extension jpg, is an image of the main class diagram of the metamodel. 

  The four previous files are in this folder for the following three metamodels:

EthereumSolidity: metamodel for the Ethereum platform, for the Solidity programming language.
HyperledgerJava: metamodel for the Hyperledger platform, for the Java programming language.
HyperledgerGo: metamodel for the Hyperledger platform, for the Go programming language.


     2. Models: It contains the different models generated in each of the levels, in this case the following:

SContractPatient.xmi: model of the SC generated with IContractML
SContractPatient.xmi: Model generated in the IContractML to EthereumSolidity Transformation.

     3. Contracts: contains the generated smart contracts, for each of the blockchain platforms and programming languages, in this case:
PatientManagement.sol: smart contract generated in EthereumSolidity.
PatientJava.java and ContractPatient.java: Contain the patient class and the contract to manage a patient, generated with the HyperledgerJava metamodel.
PatientGo.go: Smart contract generated in HyperledgerGo.

    4. Transformations: In this directory we find the m2m and m2t transformations, created in ATL and acceleo, respectively.In this directory there are two subfolders:
ATL: contains the m2m transformations, for example, Go2Java, is the transformation to transform a smart contract from Go to Java.
Acceleo: contains the m2t transformations, which are used to generate the source code of the smart contracts. Here we find 3 files with mtl extension, these are:
          - generateHyperledgerJava.mtl, generates the source code of a smart contract for the Hyperledger platform for java language.
          - generateHyperledgerGo.mtl, generates the source code of a smart contract for the Hyperledger platform for the Go language.
          - generateEthereumSolidity.mtl, generates the source code of a smart contract for the Ethereum platform for the solidity language.