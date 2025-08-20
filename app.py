"""
CLI for the AR Quiz Maker

Usage:
python app.py <input_file>

Output will be in the outputs directory. If directory does not exist, it will be created.
"""
import argparse
import os
def main():
    parser = argparse.ArgumentParser(description='AR Quiz Maker')
    parser.add_argument('input', type=str, help='input file')
    args = parser.parse_args()

    input_file_name = os.path.splitext(args.input)[0]
    output_file_name = f"{input_file_name}.html"

    classpath = "ANTLR/out/testbin:ANTLR/antlr-runtime-4.13.2.jar"
    os.system(f"java -cp {classpath} ARHtmlMain {args.input} {output_file_name}")
    
    output_dir = "outputs"
    os.makedirs(output_dir, exist_ok=True)
    
    os.system(f"mv {output_file_name} {output_dir}/{os.path.basename(output_file_name)}")

if __name__ == '__main__':
    main()