-- phpMyAdmin SQL Dump
-- version 2.11.11.3
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generato il: 26 Gen, 2017 at 06:53 PM
-- Versione MySQL: 5.0.45
-- Versione PHP: 5.2.5

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `datatest`
--

-- --------------------------------------------------------

--
-- Struttura della tabella `esempio`
--

CREATE TABLE IF NOT EXISTS `esempio` (
  `primo` int(11) default NULL,
  `secondo` decimal(6,3) default NULL,
  `terzo` time default NULL,
  `auto` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`auto`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
