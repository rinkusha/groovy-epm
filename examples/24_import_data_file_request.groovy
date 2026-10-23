/*RTPS: */
// Cube     : PlanCube (placeholder for the cube that receives the data)
// Trigger  : Jobs page
// Purpose  : load a delimited data file from the inbox into a cube with createDataImportRequest(),
//            adding to existing values and writing rejected rows to an error log in the outbox.
// Assumes  : the file is in Planning "cell format" (one data cell per line), comma separated,
//            dates written dd/MM/yyyy. createDataImportRequest()/DataImportRequest is marked ⚠ in
//            docs/01-api-reference.md — verify the setter names and the enum constants
//            (ADD_TO_EXISTING_VALUE, CELL_FORMAT) in the API reference.

Cube cube = operation.application.getCube("PlanCube")

cube.createDataImportRequest()
    .setDateFormat("dd/MM/yyyy")
    .setDataLoadOption(ADD_TO_EXISTING_VALUE)
    .setErrorFileName("Data_Load.log")
    .importDataFromFile("Data_File.txt", CELL_FORMAT, ',')

println "Import finished — check Data_Load.log in the outbox for rejected rows"
return
