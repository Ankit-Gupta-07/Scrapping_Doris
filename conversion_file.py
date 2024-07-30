import pandas as pd

import os
import pandas as pd
import rpy2.robjects as ro
import rpy2.robjects.packages as rpackages
from rpy2.robjects import pandas2ri

df = pd.read_csv("updated_data_.csv", encoding='latin1')
df.dropna(subset='Reg.No', inplace=True, ignore_index=True)
df.to_csv('Mehrauli_Amar.csv')

# Set environment variables for R
os.environ['R_HOME'] = r'C:\Program Files\R\R-4.4.1'  # Adjust this path as needed
os.environ['PATH'] = r'C:\Program Files\R\R-4.4.1\bin\x64;' + os.environ['PATH']

# Activate the pandas to R dataframe conversion
pandas2ri.activate()

# Read CSV data into a pandas DataFrame
csv_file = r"C:\Users\Shubham Das\Downloads\Freelance_webapp\Mehrauli_Amar.csv"
df = pd.read_csv(csv_file)

# Convert the pandas DataFrame to an R dataframe
r_df = pandas2ri.py2rpy(df)

# Save the R dataframe to an .RData file
# Import R's save function
base = rpackages.importr('base')

# Create a temporary environment to avoid overwriting existing variables
r_env = ro.Environment()

# Assign the R dataframe to a variable in the environment
r_env['Mehrauli_Amar'] = r_df

# Define the output .RData file path
output_rdata_file = 'Mehrauli_Amar.RData'

# Save the dataframe to the .RData file
base.save(list=ro.StrVector(['Mehrauli_Amar']), file=output_rdata_file, envir=r_env)

print(f"R dataframe has been saved to {output_rdata_file}")
