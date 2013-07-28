SELECT
   a._id, a.podcast_id, a.title, a.enclosure, a.local_file, a.entry_length, a.file_length, a.downloaded
FROM
   podcast_entry AS a
LEFT JOIN
   podcast_entry AS a2
       ON a.podcast_id = a2.podcast_id AND
       a.date_created <= a2.date_created 
GROUP BY
   a._id
HAVING
   COUNT(*) <= 2