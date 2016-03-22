Short description of the architecture.

Libary for files indesixing implemented on Java 7. The main services are IndexService, FilesHolder, SearchService, LexemeFrameManager, 
SpecFrameManager. The index is organized into two complementary approaches: inverted index on lexems and binary trees for special symbols,
which are excluded from analyzed text before lexems analysis. The Guice framework is used for DI.

Library supports multithreaded index building and multithreaded processing of search requests. Threads count and lexems size can be adjusted 
by confiduration.

Index stored in "index" directory and includes the following files:

- files.info - contains information about all files processed by library in the form of lines with description of file number in the system, 
processing status, absolute path and encoding. Component FilesHolder monitors those files and if file will removed externally, monitor will 
remove it from system too.

- spec.ctr - Control file for index of special symbols
- spec.idx - Index file for index of special symbols
- spec.idx.bak - Backup copy of index of special symbols
- lexem.ctr - Control file for index of lexems
- lexem.idx - Backup copy of index of lexems
- lexem.idx.bak - Backup copy of index of lexems

Files spec.ctr and lexem.ctr created only while backup process and needed only for system restoring. Obviously that just after creation of backup 
and indexing of ne file those files stop correspond to the main index spec.idx/lexem.idx. Thus, after abnormal termination of work index will be restored
from bak files. Component IndexService creates backup of whole index every specified period of time.