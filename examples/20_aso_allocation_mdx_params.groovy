/*RTPS: {Month}*/
// Cube     : OEP_FS (an ASO reporting cube in this example)
// Trigger  : Jobs page
// Purpose  : run an ASO allocation (Essbase "execute allocation") straight from Groovy, using
//            mdxParams() to turn an RTP into an MDX member reference.
// Source   : Oracle javadoc EpmScript.mdxParams() example

AllocationParameters p = new AllocationParameters()
p.pov = """Crossjoin(Crossjoin(Crossjoin(Crossjoin(Crossjoin(Crossjoin(
            {[No Currency]},{[No Entity]}),{[No Manager]}),{${mdxParams(rtps.Month)}}),{[BU Version_1]}),{[FY15]}),{[Bonus]})"""
p.amount                = '([No Employee], [Actual])'
p.zeroAmountOption      = ZeroAmountOption.ABORT
p.targetTimeSpanOption  = TargetTimeSpanOption.DIVIDE_AMOUNT
p.range                 = '{Descendants([Total Employees], [Total Employees].dimension.Levels(0))}'
p.target                = '([Current])'
p.basis                 = '([Salary], [Actual])'
p.basisTimeSpanOption   = BasisTimeSpanOption.COMBINE_BASIS
p.zeroBasisOption       = ZeroBasisOption.NEXT_AMOUNT
p.negativeBasisOption   = NegativeBasisOption.DEFAULT
p.allocationMethod      = AllocationMethod.SHARE
p.roundDigits           = 2
p.roundMethod           = RoundingMethod.ERRORS_TO_HIGHEST

Cube cube = operation.application.getCube("OEP_FS")
cube.executeAsoAllocation(p)
println "ASO allocation executed for ${rtps.Month.member.name}"
return
