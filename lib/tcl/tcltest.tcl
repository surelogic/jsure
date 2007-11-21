package provide tcltest 1.0

# create the "tcltest" namespace for all testing variables and procedures

namespace eval tcltest {

	# Export the public tcltest procs
	set procList [list test \
                 ]
	foreach proc $procList {
	namespace export $proc
	}
	
	proc ::tcltest::test {name description script expectedAnswer args} {
        set i [llength $args]
        if {$i == 0} {
	        set constraints {}
        } elseif {$i == 1} {
	        # "constraints" argument exists;  shuffle arguments down, then
	        # make sure that the constraints are satisfied.

	        set constraints $script
         	set script $expectedAnswer
			set expectedAnswer [lindex $args 0]

   		    #puts "Skipped $name due to constraint '$constraints'"
   		    #return 0
		}
		
		puts "Script: $script"
		
		set code [catch {uplevel $script} actualAnswer]		
	}
}
