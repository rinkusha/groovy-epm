/*RTPS: {Employee} {NewParent} {NewName}*/
// Cube     : Plan2
// Trigger  : Right-click Action Menu (Employee = clicked member; NewParent = member prompt; NewName = text prompt)
// Purpose  : metadata maintenance through Dimension.saveMember(Map). Keys mirror the metadata
//            import file columns: "Member", "Parent", "New Name", "Alias: Default", "Data Storage", ...
// Source   : Oracle javadoc "Example expression to move / rename a member"

Dimension employeeDim = rtps.Employee.member.dimension
String    mbr         = rtps.Employee.member.name

// Move
employeeDim.saveMember(["Member": mbr, "Parent": rtps.NewParent.member.name] as Map<String, Object>)

// Rename
if(rtps.NewName.enteredValue?.trim()) {
    employeeDim.saveMember(["Member": mbr, "New Name": rtps.NewName.enteredValue] as Map<String, Object>)
}

// Move + rename in one call
// employeeDim.saveMember(["Member": mbr, "New Name": rtps.NewName.enteredValue,
//                         "Parent": rtps.NewParent.member.name] as Map<String, Object>)

// Delete (removes the member AND all descendants — guard it!)
// if(rtps.Employee.member.hasChild("anything")) throwVetoException("Refusing to delete a parent")
// rtps.Employee.member.delete()

println "Moved $mbr under ${rtps.NewParent.member.name}"
// Metadata changes made via the API are visible in the outline immediately; a cube refresh
// may still be required before Essbase sees the member — run it with executeJob (see example 13).
return
