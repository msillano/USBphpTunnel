<?php	 
function sql($statment){

	$dbServer= 'localhost';
	$dbUsername = 'root';
	$dbPassword = '';		  // localhost
//	$dbPassword = 'ciromxq';  // production
	$dbDatabase = 'datatest'; 
//====================================================    
	static $connected=false;
	if(!$connected){
		/****** Connect to MySQL ******/
		if(!extension_loaded('mysql')){
			echo "** PHP is not configured to connect to MySQL on this machine.\n";
		    exit;
		}

		if(!mysql_connect($dbServer, $dbUsername, $dbPassword)){
		
			echo  '** Errore di connessione '.mysql_error()."\n";
	        exit;
			}

		/****** Connection Charset ********/
		@mysql_query("SET NAMES 'utf8'");

		/****** Select DB ********/
		if(!mysql_select_db($dbDatabase)){
		   
			echo  '** Errore in select_DB '.mysql_error()."\n";
			exit;
		}

		$connected=true;
	 }

	if(!$result = @mysql_query($statment)){
	
			echo  "** Query error in  '$statment' : ".mysql_error()."\n";
			exit;
		}

	return $result;
}
	 
/*						 
INSERT INTO  `datatest`.`esempio` (
`primo` ,
`secondo` ,
`terzo` ,
`auto`
)
VALUES (
'3',  '12.123',  '19:59', NOW( )
);

 */
// http://localhost:8080/testio/add.php?primo=5&secondo=3.4&terzo=16:05

/*
echo "<pre>";         
print_r( $_GET);
echo "</pre>";  
*/				 

$sql = 	"INSERT INTO  esempio (	 primo ,  secondo ,	terzo) VALUES (	".$_GET['primo'].", ".$_GET['secondo'].", '".$_GET['terzo']."'); " ;
sql($sql);

echo "D 015 0x2 \n";
echo "P 0x0E  -34567\n";
 ?>
