/*RTPS: */
// Cube     : PlanCube (placeholder; replace with the target cube)
// Trigger  : Jobs page
// Purpose  : load a comma-delimited Planning cell-format file from the inbox, add values to
//            existing data, and write rejected rows to an outbox error file.
// Assumes  : Data_File.txt has no header; each row follows the target cube's dimension order
//            (Scenario, Version, Year, Period, Entity, Currency, Account), then the cell value.
//            See 24_import_data_file_request_sample.txt; replace its members/dimensions. Date
//            values use dd/MM/yyyy.

final String CUBE = "PlanCube"
final String DATA_FILE = "Data_File.txt"
final String ERROR_FILE = "Data_Load.log"

Cube cube = operation.application.getCube(CUBE)
cube.createDataImportRequest()
    .setDateFormat("dd/MM/yyyy")
    .setDataLoadOption(ADD_TO_EXISTING_VALUE)
    .setErrorFileName(ERROR_FILE)
    .importDataFromFile(DATA_FILE, CELL_FORMAT, ',')

println "Import finished; check $ERROR_FILE in the outbox for rejected rows"
return
