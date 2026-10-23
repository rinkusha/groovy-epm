# 04 — Official Oracle sources

## API reference (javadoc)
- Groovy Rules Java API Reference (frames): https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groov/
- Overview / "Using the APIs": https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groov/overview-summary.html
- All classes: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groov/allclasses-noframe.html
- A–Z method index: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groov/index-all.html
- Oracle's example scripts: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groov/groovy-examples.html
- Key classes (base URL `…/groov/oracle/epm/api/`):
  - `script/EpmScript.html`
  - `model/Operation.html`, `model/Application.html`, `model/Cube.html`, `model/Dimension.html`, `model/Member.html`, `model/RtpValue.html`
  - `grid/DataGrid.html`, `grid/DataGrid.DataCell.html`, `grid/DataGridDefinitionBuilder.html`, `grid/DataGridBuilder.html`, `grid/FlexibleDataGridDefinitionBuilder.html`, `grid/GridIterator.html`
  - `model/DataMap.html`, `model/SmartPush.html`, `model/JobFactory.html`, `model/Job.html`
  - `model/rest/HttpRequest.html`, `model/rest/HttpRequestWithBody.html`, `model/rest/HttpResponse.html`, `model/Connection.html`
  - `model/DataExporter.html`, `model/DataImporter.html`, `model/CopyDataRequest.html`, `model/AllocationParameters.html`, `model/excel/Excel.html`

## Tutorials (full scripts; the examples/ folder is derived from these)
- Index: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/ecalc/groovy_business_rules_tutorials.html
- Introduction: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-introduction/index.html
- Data grids & iterators: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-working-with-data-grids-and-iterators/index.html
- Moving members: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-moving-dimension-members-with-groovy/index.html
- Smart Push: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-moving-modified-data-using-groovy-and-smartpush/index.html
- Validating data entry: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-validating-data-entry-values-with-groovy/index.html
- Adding members: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-adding-dimension-members/index.html
- Calculating modified data: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-calculating-modified-data/index.html
- Strategic Modeling integration: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-integrating-data/index.html
- Templates with DTPs: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-creating-templates/index.html
- EDMCS → Planning metadata via Groovy: https://docs.oracle.com/en/cloud/saas/enterprise-data-management-cloud/tutorial-integrate-metadata/index.html
- Internal REST (Data Management): https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-calling-internal-rest-api/index.html
- External REST: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/groovy-tutorial-calling-an-external-rest-api/index.html

## EPM Automate inside Groovy
- Running commands without installing EPM Automate: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/cepma/epm_auto_groovy.html
- Methods (`getEpmAutomate`, `execute`, `getStatus`, `getOutput`, `getItemsList`): https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/cepma/epm_auto_groovy_methods.html
- Supported commands: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/cepma/epm_auto_groovy_supported_commands.html
- Clone example: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/cepma/epm_auto_groovy_clone.html
- EPM Automate command reference: https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/cepma/toc.htm

## REST APIs you will call from Groovy
- Planning REST (jobs, import/export metadata, data, substitution variables): https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/prest/
- Data Management/Data Integration REST (`/aif/rest/V1/jobs`): same book, "Data Management" chapter
- EDMCS REST: https://docs.oracle.com/en/cloud/saas/enterprise-data-management-cloud/edmra/

## Calculation Manager (how rules are created/deployed)
- Designing with Calculation Manager (Groovy chapter): https://docs.oracle.com/en/cloud/saas/enterprise-performance-management-common/ecalc/
