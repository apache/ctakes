# Purpose of this script is to strip the typesystems from a TypeSystem file and
# get an output with just the typesystem name and location

typeFile = open('resources/TypeSystem.xml', 'r')
Lines = typeFile.readlines()

# count = 0
outputFile = open('ctakes_types.py', 'w')
# Strips the newline character
lines_seen = set()
for line in Lines:
    if line.strip()[0:6] == "<name>":
        if len(line.strip().split('.')) > 1:
            if line not in lines_seen:
                # count += 1
                step_0 = line.split(">")
                step_1 = step_0[1]
                step_2 = step_1.split("<")
                step_3 = step_2[0]
                step_4 = step_3.split('.')
                step_5 = step_4[len(step_4)-1]

                # added to remove the chance of adding a duplicate to the file
                lines_seen.add(line)

                # writing to file
                outputFile.writelines(step_5 + " = '" + step_3 + "'\n")

                # print("Line{}: {}".format(count, step_5))

typeFile.close()
outputFile.close()