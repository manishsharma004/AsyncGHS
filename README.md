# Floodmax Synchronous - How to Run

1. Unzip the folder and cd to root.
2. Run the file run.sh, it should compile and show the output with the sample input provided.

To run with custom input file, use the command below:
java src/MainDriver <number_of_processes> --path <path_to_file>

Format of input file:
First line should have UNIQUE, integer ids for processes separated by a tab (\t).
The next line should be blank.
The next <number_of_processes> line should have adjacent matrix separated by tabs. The order of elements correspond to the order of the unique ids provided.
