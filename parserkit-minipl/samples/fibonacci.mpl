var n : int;
print "Which fibonacci number? ";
read n;

var a : int := 0;
var b : int := 1;
var i : int;
for i in 0..n do
    var tmp : int := a;
    a := a + b;
    b := tmp;
end for;

print a;
print "\n";
