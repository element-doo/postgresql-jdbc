#!/bin/sh
#
# $PostgreSQL: pgjdbc/update-translations.sh,v 1.4 2004/11/07 22:15:24 jurka Exp $

ant clean
find . -name '*.java' -o -name '*.java.in' > translation.filelist
xgettext -k -kGT.tr -F -f translation.filelist -L Java -o org/postgresql/translation/messages.pot
rm translation.filelist

for i in org/postgresql/translation/*.po
do
	msgmerge -U $i org/postgresql/translation/messages.pot
	polang=`basename $i .po`
	msgfmt -j -l $polang -r org.postgresql.translation.messages -d . $i
done
