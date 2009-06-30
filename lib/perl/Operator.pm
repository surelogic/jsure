#!/usr/bin/perl

package Operator;
use FileHandle;

sub new {
  my ($package, $filename) = @_;
  my %hash;
  die("missing filename for operator") unless defined $filename;
  
  %hash = (FILENAME => "$filename", 
           FILEHANDLE => "",
           WORDS => (),
           MODIFIERS => "public",
           OPNAME => "MyOperator",
           ISROOT => "false",
           SUPEROP => "$optype",  # DEFINE this
           SYNTAX => (),
           TYPES => (),    # types for children and slots
           PRECS => (),    # precedence for children (and slots?)
           NUMTOTAL => 0,
           NUMCHILDREN => 0,
           NUMSLOTS => 0,
           VARIABLE => "false",
           VARIABILITY => "",  # Whether it's a "+" or "*" or other variant
           VARIABLEINDEX => -1, # Only one variable child allowed, so one index 
           PRECEDENCE => "false",  
           BEFORE => "",
           AFTEROP => "",
           AFTER => ""         
           );
  bless \%hash => $package;
}

sub parse {
  my ($self) = @_;
  my $filename = $self->{FILENAME};
  my $fileHandle = new FileHandle;
  my $before = "";
  my $after = "";
  
  $fileHandle->open($filename) or die "Could not read operator file $filename\n";
  $self->{FILEHANDLE} = $fileHandle;
  
  print ("starting\n");
  
  # Copy everything before the operator definition
  while (($line = <$fileHandle>) && !($line =~ /^[a-z ]*operator/)) {
  	#print ("read $line \n");
    if (!($line =~ /^#/)) {
      $before = $before . $line;
    }
  }
  $self->parseOperatorLine($line);
  $self->parseSyntax();

  # Copy the rest of the file  
  while ($line = <$fileHandle>) {
    $after = $after . $line;
  }
  $fileHandle->close();

  $self->{BEFORE} = $before;
  $self->{AFTER} = $after;
}

# Match against the beginning of the operator definition
sub parseOperatorLine {
  my ($self, $line) = @_;		
  my $afterOp = "";

  print ("starting operator line $line\n");
  if ($line =~ /^([a-z ]*)operator[ \t]*([A-Za-z][A-Za-z0-9_]+)(.*)/) {
  	$self->{MODIFIERS} = $1;
    $self->{OPNAME} = $2;
    $line = $3;
      
    $self->{WORDS} = split(/\s+/,$line);
    
    if (($word = $self->getword()) eq "extends") {
      $self->{SUPEROP} = $self->getword();       
    } else {
      # No extends clause, so it is a root
      $self->{ISROOT} = "true";
      
      # Insert IAcceptor as an implemented interface
      $afterOp = "$word ";
#      if ($word eq "implements") {
#        &printjava("implements IAcceptor, ") if ($genvisitor eq "true");
#      } else {
#        &printjava("implements IAcceptor ") if ($genvisitor eq "true");
#        &printjava("$word ");
#      }
    }      
    # Copy everything else up to open brace of operator definition
    until ($word eq "{") {
	    $word = $self->getword();
	    print ("got $word \n");
      $afterOp = $afterOp . "$word ";
    }
  }	
  print ("done with operator line\n");
}

sub getword {
  my ($self) = @_;			
  my $fileHandle = $self->{FILEHANDLE};
  my $words = $self->{WORDS};
  my $word = "";
  my $line = "";
  
  if (@words == 0 || $words[0] =~ m|^//|) {
    $line = <$fileHandle> || die ("operator file ended early");
    print ("line = $line");
    # (@_ == 0) && &printjava("\n    ");
    @words = split(/\s+/,$line);
    $self->{WORDS} = @words;
    $word = &getword;
  } elsif ($words[0] eq "") {
    shift @words;
    $self->{WORDS} = @words;
    $word = &getword;
  } else {
    $word = shift @words;
    $self->{WORDS} = @words;
  }
  print "word = $word \n";
  $word;
}

sub parseSyntax {
  my ($self) = @_;	
  my @open = ();	# stack of indices of unmatched left parens
  
  # These need to be copied to fields
  my @syntax = ();
  my @types = ();    # types for children and slots
  my @precs = ();    # precedence for children (and slots?)
  #my @balance = ();	# "pointers" within syntax to matching ( )'s
  my $total = 0;
  my $children = 0;
  my $slots = 0;
  my $variable = "false";
  my $variability = "";  # Whether it's a "+" or "*" or other variant
  my $variablechild = -1; # Only one variable child allowed, so one index 
  my $precedence = "false";
  
  # Look for start of "syntax" block
  if (($word = $self->getword("no echo")) eq "syntax") {
  	# Confirm opening brace
    &getword("no echo") eq "{" || die ("expected { in syntax");
  
    # Loop until we find the closing brace
	  until (($word = $self->getword("no echo")) eq "}") {	
	    push(@syntax,$word);
	    push(@types,"");
	    push(@precs,"");
	  
	    if ($word eq "*" || $word eq "**" || $word eq "*/" || 
          $word eq "+" || $word eq "++" || $word eq "+/") {
        # Deal with variability tags
	      $variable = "true";
	      $variability = $word;                           
	    } elsif ($word eq "?" || $word eq "??" || $word eq "?/") {
	  	  # Deal with option tags
	      ($variability =~ "[+]") || 
		     die("? and ?/ only permitted for + variable syntax");
	      $variability = $word;
	    } elsif ($word eq "(") {
	      push(@open,$total+1);
	    } elsif ($word eq ")" ) {
	      if ($popped = pop(@open)) {
	        $popped -= 1;
	        $balance[$popped] = $total;
	        $balance[$total] = $popped;
	      } else {
	        die("unmatched right parenthesis");
	      }
	    } elsif ($word =~ /^<.*>$/) {             
	      # Not currently doing anything with "<tag>"	    
	    } elsif ($word =~ /^([a-zA-Z][A-Za-z0-9_]+:)?([A-Za-z][A-Za-z0-9_]+)(\([a-zA-Z0-9_]+\))?$/) {
	  	  # Match against "foo:FooChild"
	      if ($1 eq "") {
   	      # Default to child1, child2, etc.
	        $syntax[$total] = "child$children";
	      } else {
	        $syntax[$total] = $1;
	        chop($syntax[$total]);
	      }      
                      
	      $types[$total] = $2;
	      $precs[$total] = $3;
	      if ($3 ne "") {
	        $precedence = "true";
	      }
	      $variablechild = $total;
	      $children += 1;
      } elsif ($word =~ /^\$([a-zA-Z][A-Za-z0-9_]+:)?([A-Za-z][A-Za-z0-9_]+)$/) {
	  	  # Match against "$foo:FooType"
	      if ($1 eq "") {
   	      # Default to $slot1, $slot2, etc.
	        $syntax[$total] = "\$slot$slots";
	      } else {
	        $syntax[$total] = "\$" . $1;
	        chop($syntax[$total]);
	      }
	      $types[$total] = $2;
  	    $slots += 1;
      } elsif (!($word =~ /^\"/)) {
	      die("bad syntax element $word");
	    }
	    $total += 1;    
    }
    if (pop(@open)) {
    	die("unmatched left parenthesis");
    } 
  
    $self->{SYNTAX} = @syntax;
    $self->{TYPES} = @types;
    $self->{PRECS} = @precs;
    $self->{NUMTOTAL} = $total;
    $self->{NUMCHILDREN} = $children;
    $self->{NUMSLOTS} = $slots;  
    $self->{VARIABLE} = $variable;  
    $self->{VARIABILITY} = $variability;  
    $self->{VARIABLEINDEX} = $variablechild;  
    $self->{PRECEDENCE} = $precedence;   
  }
}

1;