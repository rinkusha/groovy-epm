/*RTPS: {Scenario} {Year}*/
// Cube     : Plan1
// Trigger  : Jobs page (no form needed)
// Purpose  : READ pattern — query a slice of the cube like a Smart View retrieve, iterate the
//            cells, use crossDimCell to pick sibling accounts, and sum per entity.
// Builder shape: setPov(dims, [[m],[m]...]) / addColumn(dims, [[m,m,...]]) / addRow(dims, [[m,...]])
//                lists-of-lists; member functions like ILvl0Descendants("X") are allowed.

Cube cube = operation.application.getCube("Plan1")

DataGridDefinitionBuilder b = cube.dataGridDefinitionBuilder()
b.setPov(['Scenario', 'Years', 'Version', 'Currency'],
         [[rtps.Scenario.member.name], [rtps.Year.member.name], ['Working'], ['USD']])
b.addColumn(['Period'],  [['YearTotal']])
b.addColumn(['Account'], [['Revenue', 'COGS']])             // second column segment
b.addRow(['Entity'],     [['ILvl0Descendants("Total Entity")']])

Map<String, BigDecimal> marginByEntity = [:]

// second arg false = keep #MISSING cells in the grid
cube.loadGrid(b.build(), false).withCloseable { DataGrid grid ->
    grid.dataCellIterator('Revenue').each { DataCell rev ->
        DataCell cogs = rev.crossDimCell('COGS')
        if(rev.missing) return                              // skip empty entities
        String entity = rev.getMemberName('Entity')
        double cogsVal = (cogs == null || cogs.missing) ? 0d : cogs.data   // crossDimCell can return null
        marginByEntity[entity] = (rev.data - cogsVal) as BigDecimal
    }
}

marginByEntity.sort { -it.value }.each { entity, margin ->
    printf("%-30s %,15.2f%n", entity, margin)
}
return
