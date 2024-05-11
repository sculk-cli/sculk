import os
import re

COMMANDS_PACKAGE = "./src/main/kotlin/commands"
# CliktCommand\(name\s*=\s*"(\w*)",\s*help\s*=\s*"([\w\s]*)"
REGEX_NAME_HELP = r"CliktCommand\(name\s*=\s*\"(\w*)\",\s*help\s*=\s*\"([\w\s]*)\""

if not os.path.exists(COMMANDS_PACKAGE):
		raise Exception("Not in a Gradle project directory")

def generate_command_docs(command_file):
	matches = re.findall(REGEX_NAME_HELP, command_file)
	if not matches:
			return
	command_name, command_help = matches[0]

	return command_name, f"_{command_help}_\n"

def main():
	for file in os.listdir(COMMANDS_PACKAGE):
		if not file.endswith(".kt"):
				continue
		file_path = os.path.join(COMMANDS_PACKAGE, file)
		file_contents = open(file_path).read()
		result = generate_command_docs(file_contents)
		
		if result:
			name, docs = result
			docs_path = os.path.join("./docgen", f"{name}.md")
			os.makedirs(os.path.dirname(docs_path), exist_ok=True)
			with open(docs_path, "w") as f:
				f.write(docs)


if __name__ == "__main__":
	main()
