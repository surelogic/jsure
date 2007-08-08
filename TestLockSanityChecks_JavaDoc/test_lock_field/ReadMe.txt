The classes in this package are designed to test the following rules for
lock declarations (including policy lock declarations):
(1) The associated field must exist
(2) Field must be "this", "class", or a final field
(2a) Field must not be an instance field from a non-ancestor class
(2b) Field must not be a primitive type
